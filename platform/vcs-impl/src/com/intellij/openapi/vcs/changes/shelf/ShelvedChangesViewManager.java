// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.vcs.changes.shelf;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffContentFactoryEx;
import com.intellij.diff.chains.DiffRequestProducerException;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.impl.CacheDiffRequestProcessor;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.ide.DeleteProvider;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.EditSourceAction;
import com.intellij.ide.dnd.*;
import com.intellij.ide.dnd.aware.DnDAwareTree;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.ListSelection;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.diff.DiffBundle;
import com.intellij.openapi.diff.impl.patch.BaseRevisionTextPatchEP;
import com.intellij.openapi.diff.impl.patch.PatchEP;
import com.intellij.openapi.diff.impl.patch.TextFilePatch;
import com.intellij.openapi.diff.impl.patch.apply.PlainSimplePatchApplier;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.util.BackgroundTaskUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.registry.RegistryValue;
import com.intellij.openapi.util.registry.RegistryValueListener;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.*;
import com.intellij.openapi.vcs.changes.*;
import com.intellij.openapi.vcs.changes.actions.ShowDiffPreviewAction;
import com.intellij.openapi.vcs.changes.patch.PatchFileType;
import com.intellij.openapi.vcs.changes.patch.tool.PatchDiffRequest;
import com.intellij.openapi.vcs.changes.ui.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.pom.Navigatable;
import com.intellij.pom.NavigatableAdapter;
import com.intellij.ui.*;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.impl.ContentImpl;
import com.intellij.util.Consumer;
import com.intellij.util.IconUtil;
import com.intellij.util.IconUtil.IconSizeWrapper;
import com.intellij.util.PathUtil;
import com.intellij.util.concurrency.annotations.RequiresEdt;
import com.intellij.util.containers.UtilKt;
import com.intellij.util.text.DateFormatUtil;
import com.intellij.util.ui.GraphicsUtil;
import com.intellij.util.ui.tree.TreeUtil;
import com.intellij.util.ui.update.MergingUpdateQueue;
import com.intellij.util.ui.update.Update;
import com.intellij.vcsUtil.VcsUtil;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.intellij.icons.AllIcons.Vcs.Patch_applied;
import static com.intellij.openapi.vcs.VcsNotificationIdsHolder.SHELVE_DELETION_UNDO;
import static com.intellij.openapi.vcs.changes.shelf.DiffShelvedChangesActionProvider.createAppliedTextPatch;
import static com.intellij.openapi.vcs.changes.ui.ChangesGroupingSupport.REPOSITORY_GROUPING;
import static com.intellij.openapi.vcs.changes.ui.ChangesViewContentManager.*;
import static com.intellij.openapi.vcs.changes.ui.ChangesViewContentManagerKt.isCommitToolWindowShown;
import static com.intellij.util.FontUtil.spaceAndThinSpace;
import static com.intellij.util.ObjectUtils.chooseNotNull;
import static com.intellij.util.containers.ContainerUtil.*;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;

// open for Rider
public class ShelvedChangesViewManager implements Disposable {
  private static final Logger LOG = Logger.getInstance(ShelvedChangesViewManager.class);
  @NonNls static final String SHELF_CONTEXT_MENU = "Vcs.Shelf.ContextMenu";
  private static final String SHELVE_PREVIEW_SPLITTER_PROPORTION = "ShelvedChangesViewManager.DETAILS_SPLITTER_PROPORTION"; //NON-NLS

  private final ShelveChangesManager myShelveChangesManager;
  private final Project myProject;
  private ShelfToolWindowPanel myPanel = null;
  private ContentImpl myContent = null;
  private final MergingUpdateQueue myUpdateQueue;
  private final List<Runnable> myPostUpdateEdtActivity = new ArrayList<>();

  public static final DataKey<ChangesTree> SHELVED_CHANGES_TREE =
    DataKey.create("ShelveChangesManager.ShelvedChangesTree");
  public static final DataKey<List<ShelvedChangeList>> SHELVED_CHANGELIST_KEY =
    DataKey.create("ShelveChangesManager.ShelvedChangeListData");
  public static final DataKey<List<ShelvedChangeList>> SHELVED_RECYCLED_CHANGELIST_KEY =
    DataKey.create("ShelveChangesManager.ShelvedRecycledChangeListData");
  public static final DataKey<List<ShelvedChangeList>> SHELVED_DELETED_CHANGELIST_KEY =
    DataKey.create("ShelveChangesManager.ShelvedDeletedChangeListData");
  public static final DataKey<List<ShelvedChange>> SHELVED_CHANGE_KEY = DataKey.create("ShelveChangesManager.ShelvedChange");
  public static final DataKey<List<ShelvedBinaryFile>> SHELVED_BINARY_FILE_KEY = DataKey.create("ShelveChangesManager.ShelvedBinaryFile");

  public static ShelvedChangesViewManager getInstance(Project project) {
    return project.getService(ShelvedChangesViewManager.class);
  }

  public ShelvedChangesViewManager(Project project) {
    myProject = project;
    myShelveChangesManager = ShelveChangesManager.getInstance(project);
    myUpdateQueue = new MergingUpdateQueue("Update Shelf Content", 200, true, null, myProject, null, true);

    project.getMessageBus().connect().subscribe(ShelveChangesManager.SHELF_TOPIC, e -> scheduleContentUpdate());
  }

  private void scheduleContentUpdate() {
    myUpdateQueue.queue(new MyContentUpdater());
  }

  private void updateTreeIfShown(@NotNull Consumer<? super ShelfTree> treeConsumer) {
    if (myContent == null) return;
    treeConsumer.consume(myPanel.myTree);
  }

  @RequiresEdt
  void updateViewContent() {
    if (myShelveChangesManager.getAllLists().isEmpty()) {
      if (myContent != null) {
        removeContent(myContent);
        VcsNotifier.getInstance(myProject).hideAllNotificationsByType(ShelfNotification.class);
      }
      myContent = null;
    }
    else {
      if (myContent == null) {
        myPanel = new ShelfToolWindowPanel(myProject);
        myContent = new ContentImpl(myPanel.myRootPanel, VcsBundle.message("shelf.tab"), false);
        myContent.setTabName(SHELF); //NON-NLS overridden by displayName above
        MyDnDTarget dnDTarget = new MyDnDTarget(myPanel.myProject, myContent);
        myContent.putUserData(Content.TAB_DND_TARGET_KEY, dnDTarget);
        myContent.putUserData(IS_IN_COMMIT_TOOLWINDOW_KEY, true);

        myContent.setCloseable(false);
        myContent.setDisposer(myPanel);
        DnDSupport.createBuilder(myPanel.myTree)
          .setImageProvider(myPanel::createDraggedImage)
          .setBeanProvider(myPanel::createDragStartBean)
          .setTargetChecker(dnDTarget)
          .setDropHandler(dnDTarget)
          .setDisposableParent(myContent)
          .install();
        addContent(myContent);
      }
      updateTreeIfShown(tree -> {
        tree.rebuildTree();
      });
    }
  }

  protected void removeContent(Content content) {
    ChangesViewContentI contentManager = ChangesViewContentManager.getInstance(myProject);
    contentManager.removeContent(content);
    contentManager.selectContent(ChangesViewContentManager.LOCAL_CHANGES);
  }

  protected void addContent(Content content) {
    ChangesViewContentI contentManager = ChangesViewContentManager.getInstance(myProject);
    contentManager.addContent(content);
  }

  protected void activateContent() {
    ChangesViewContentI contentManager = ChangesViewContentManager.getInstance(myProject);
    contentManager.setSelectedContent(myContent);

    ToolWindow window = getToolWindowFor(myProject, SHELF);
    if (window != null && !window.isVisible()) {
      window.activate(null);
    }
  }

  private static final class MyShelvedTreeModelBuilder extends TreeModelBuilder {
    private MyShelvedTreeModelBuilder(Project project, @NotNull ChangesGroupingPolicyFactory grouping) {
      super(project, grouping);
    }

    public void setShelvedLists(@NotNull List<ShelvedChangeList> shelvedLists) {
      createShelvedListsWithChangesNode(shelvedLists, myRoot);
    }

    public void setDeletedShelvedLists(@NotNull List<ShelvedChangeList> shelvedLists) {
      createShelvedListsWithChangesNode(shelvedLists, createTagNode(VcsBundle.message("shelve.recently.deleted.node")));
    }

    private void createShelvedListsWithChangesNode(@NotNull List<ShelvedChangeList> shelvedLists, @NotNull ChangesBrowserNode<?> parentNode) {
      shelvedLists.forEach(changeList -> {
        List<ShelvedWrapper> shelvedChanges = new ArrayList<>();
        requireNonNull(changeList.getChanges()).stream().map(ShelvedWrapper::new).forEach(shelvedChanges::add);
        changeList.getBinaryFiles().stream().map(ShelvedWrapper::new).forEach(shelvedChanges::add);

        shelvedChanges.sort(comparing(s -> s.getChange(myProject), CHANGE_COMPARATOR));

        ShelvedListNode shelvedListNode = new ShelvedListNode(changeList);
        insertSubtreeRoot(shelvedListNode, parentNode);
        for (ShelvedWrapper shelved : shelvedChanges) {
          Change change = shelved.getChange(myProject);
          FilePath filePath = ChangesUtil.getFilePath(change);
          insertChangeNode(change, shelvedListNode, new ShelvedChangeNode(shelved, filePath, change.getOriginText(myProject)));
        }
      });
    }
  }

  @RequiresEdt
  private void updateTreeModel() {
    updateTreeIfShown(tree -> tree.setPaintBusy(true));
    BackgroundTaskUtil.executeOnPooledThread(myProject, () -> {
      List<ShelvedChangeList> lists = myShelveChangesManager.getAllLists();
      lists.forEach(l -> l.loadChangesIfNeeded(myProject));
      List<ShelvedChangeList> sortedLists = sorted(lists, ChangelistComparator.getInstance());
      ApplicationManager.getApplication().invokeLater(() -> {
        updateViewContent();
        updateTreeIfShown(tree -> {
          tree.setLoadedLists(sortedLists);
          tree.setPaintBusy(false);
          tree.rebuildTree();
        });
        myPostUpdateEdtActivity.forEach(Runnable::run);
        myPostUpdateEdtActivity.clear();
      }, ModalityState.NON_MODAL, myProject.getDisposed());
    });
  }

  @RequiresEdt
  public void startEditing(@NotNull ShelvedChangeList shelvedChangeList) {
    runAfterUpdate(() -> {
      selectShelvedList(shelvedChangeList);
      updateTreeIfShown(tree -> tree.startEditingAtPath(tree.getLeadSelectionPath()));
    });
  }

  static class ChangelistComparator implements Comparator<ShelvedChangeList> {
    private final static ChangelistComparator ourInstance = new ChangelistComparator();

    public static ChangelistComparator getInstance() {
      return ourInstance;
    }

    @Override
    public int compare(ShelvedChangeList o1, ShelvedChangeList o2) {
      return o2.DATE.compareTo(o1.DATE);
    }
  }

  public void activateView(@Nullable final ShelvedChangeList list) {
    runAfterUpdate(() -> {
      if (myContent == null) return;

      if (list != null) {
        selectShelvedList(list);
      }
      activateContent();
    });
  }

  private void runAfterUpdate(@NotNull Runnable postUpdateRunnable) {
    GuiUtils.invokeLaterIfNeeded(() -> {
      myUpdateQueue.cancelAllUpdates();
      myPostUpdateEdtActivity.add(postUpdateRunnable);
      updateTreeModel();
    }, ModalityState.NON_MODAL, myProject.getDisposed());
  }

  @Override
  public void dispose() {
    myUpdateQueue.cancelAllUpdates();
  }

  public void closeEditorPreview() {
    ApplicationManager.getApplication().assertIsDispatchThread();

    if (myContent == null) {
      return;
    }

    DiffPreview diffPreview = myPanel.myDiffPreview;
    if (diffPreview instanceof EditorTabPreview) {
      ((EditorTabPreview)diffPreview).closePreview();
    }
  }

  public void openEditorPreview() {
    ApplicationManager.getApplication().assertIsDispatchThread();

    if (myContent == null) return;
    myPanel.openEditorPreview(false);
  }

  public void updateOnVcsMappingsChanged() {
    ApplicationManager.getApplication().invokeLater(() -> {
      updateTreeIfShown(tree -> {
        ChangesGroupingSupport treeGroupingSupport = tree.getGroupingSupport();
        if (treeGroupingSupport.isAvailable(REPOSITORY_GROUPING) && treeGroupingSupport.get(REPOSITORY_GROUPING)) {
          tree.rebuildTree();
        }
      });
    }, myProject.getDisposed());
  }

  public void selectShelvedList(@NotNull ShelvedChangeList list) {
    updateTreeIfShown(tree -> {
      DefaultMutableTreeNode treeNode = TreeUtil.findNodeWithObject((DefaultMutableTreeNode)tree.getModel().getRoot(), list);
      if (treeNode == null) {
        LOG.warn(VcsBundle.message("shelve.changelist.not.found", list.DESCRIPTION));
        return;
      }
      TreeUtil.selectNode(tree, treeNode);
    });
  }

  private static final class ShelfTree extends ChangesTree {
    private List<ShelvedChangeList> myLoadedLists = emptyList();
    private final DeleteProvider myDeleteProvider = new MyShelveDeleteProvider(myProject, this);

    private ShelfTree(@NotNull Project project) {
      super(project, false, false, true);
      setKeepTreeState(true);
      setDoubleClickHandler(e -> showShelvedChangesDiff());
      setEnterKeyHandler(e -> showShelvedChangesDiff());
    }

    public void setLoadedLists(@NotNull List<ShelvedChangeList> lists) {
      myLoadedLists = new ArrayList<>(lists);
    }

    @Override
    public boolean isPathEditable(TreePath path) {
      return isEditable() && getSelectionCount() == 1 && path.getLastPathComponent() instanceof ShelvedListNode;
    }

    @NotNull
    @Override
    protected ChangesGroupingSupport installGroupingSupport() {
      return new ChangesGroupingSupport(myProject, this, false);
    }

    @Override
    public int getToggleClickCount() {
      return 2;
    }

    private boolean showShelvedChangesDiff() {
      if (!hasExactlySelectedChanges()) return false;
      DiffShelvedChangesActionProvider.showShelvedChangesDiff(DataManager.getInstance().getDataContext(this));
      return true;
    }

    private boolean hasExactlySelectedChanges() {
      return !UtilKt.isEmpty(VcsTreeModelData.exactlySelected(this).userObjectsStream(ShelvedWrapper.class));
    }

    @Override
    public void rebuildTree() {
      boolean showRecycled = ShelveChangesManager.getInstance(myProject).isShowRecycled();
      MyShelvedTreeModelBuilder modelBuilder = new MyShelvedTreeModelBuilder(myProject, getGrouping());
      modelBuilder.setShelvedLists(filter(myLoadedLists, l -> !l.isDeleted() && (showRecycled || !l.isRecycled())));
      modelBuilder.setDeletedShelvedLists(filter(myLoadedLists, ShelvedChangeList::isDeleted));
      updateTreeModel(modelBuilder.build());
    }

    @Nullable
    @Override
    public Object getData(@NotNull @NonNls String dataId) {
      if (SHELVED_CHANGES_TREE.is(dataId)) {
        return this;
      }
      else if (SHELVED_CHANGELIST_KEY.is(dataId)) {
        return new ArrayList<>(getSelectedLists(this, l -> !l.isRecycled() && !l.isDeleted()));
      }
      else if (SHELVED_RECYCLED_CHANGELIST_KEY.is(dataId)) {
        return new ArrayList<>(getSelectedLists(this, l -> l.isRecycled() && !l.isDeleted()));
      }
      else if (SHELVED_DELETED_CHANGELIST_KEY.is(dataId)) {
        return new ArrayList<>(getSelectedLists(this, l -> l.isDeleted()));
      }
      else if (SHELVED_CHANGE_KEY.is(dataId)) {
        return StreamEx.of(VcsTreeModelData.selected(this).userObjectsStream(ShelvedWrapper.class)).map(s -> s.getShelvedChange())
          .nonNull().toList();
      }
      else if (SHELVED_BINARY_FILE_KEY.is(dataId)) {
        return StreamEx.of(VcsTreeModelData.selected(this).userObjectsStream(ShelvedWrapper.class)).map(s -> s.getBinaryFile())
          .nonNull().toList();
      }
      else if (VcsDataKeys.HAVE_SELECTED_CHANGES.is(dataId)) {
        return getSelectionCount() > 0;
      }
      else if (VcsDataKeys.CHANGES.is(dataId)) {
        List<ShelvedWrapper> shelvedChanges = VcsTreeModelData.selected(this).userObjects(ShelvedWrapper.class);
        if (!shelvedChanges.isEmpty()) {
          return map2Array(shelvedChanges, Change.class, s -> s.getChange(myProject));
        }
      }
      else if (PlatformDataKeys.DELETE_ELEMENT_PROVIDER.is(dataId)) {
        return myDeleteProvider;
      }
      else if (CommonDataKeys.NAVIGATABLE_ARRAY.is(dataId)) {
        List<ShelvedWrapper> shelvedChanges = VcsTreeModelData.selected(this).userObjects(ShelvedWrapper.class);
        final ArrayDeque<Navigatable> navigatables = new ArrayDeque<>();
        for (final ShelvedWrapper shelvedChange : shelvedChanges) {
          if (shelvedChange.getBeforePath() != null && !FileStatus.ADDED.equals(shelvedChange.getFileStatus())) {
            final NavigatableAdapter navigatable = new NavigatableAdapter() {
              @Override
              public void navigate(boolean requestFocus) {
                final VirtualFile vf = shelvedChange.getBeforeVFUnderProject(myProject);
                if (vf != null) {
                  navigate(myProject, vf, true);
                }
              }
            };
            navigatables.add(navigatable);
          }
        }
        return navigatables.toArray(Navigatable.EMPTY_NAVIGATABLE_ARRAY);
      }
      return super.getData(dataId);
    }
  }

  @NotNull
  private static Set<ShelvedChangeList> getSelectedLists(@NotNull ChangesTree tree,
                                                         @NotNull Predicate<? super ShelvedChangeList> condition) {
    TreePath[] selectionPaths = tree.getSelectionPaths();
    if (selectionPaths == null) return Collections.emptySet();
    return StreamEx.of(selectionPaths)
      .map(path -> TreeUtil.findObjectInPath(path, ShelvedChangeList.class))
      .filter(Objects::nonNull)
      .filter(condition)
      .collect(Collectors.toSet());
  }

  @NotNull
  static ListSelection<ShelvedWrapper> getSelectedChangesOrAll(@NotNull DataContext dataContext) {
    ChangesTree tree = dataContext.getData(SHELVED_CHANGES_TREE);
    if (tree == null) return ListSelection.createAt(Collections.emptyList(), 0);

    ListSelection<ShelvedWrapper> wrappers = ListSelection.createAt(VcsTreeModelData.selected(tree).userObjects(ShelvedWrapper.class), 0);

    if (wrappers.getList().size() == 1) {
      // return all changes for selected changelist
      ShelvedChangeList changeList = getFirstItem(getSelectedLists(tree, it -> true));
      if (changeList != null) {
        ChangesBrowserNode<?> changeListNode = (ChangesBrowserNode<?>)TreeUtil.findNodeWithObject(tree.getRoot(), changeList);
        if (changeListNode != null) {
          List<ShelvedWrapper> allWrappers = changeListNode.getAllObjectsUnder(ShelvedWrapper.class);
          if (allWrappers.size() > 1) {
            ShelvedWrapper toSelect = getFirstItem(wrappers.getList());
            return ListSelection.create(allWrappers, toSelect);
          }
        }
      }
    }
    return wrappers;
  }

  @NotNull
  public static List<ShelvedChangeList> getShelvedLists(@NotNull final DataContext dataContext) {
    List<ShelvedChangeList> shelvedChangeLists = new ArrayList<>();
    shelvedChangeLists.addAll(notNullize(SHELVED_CHANGELIST_KEY.getData(dataContext)));
    shelvedChangeLists.addAll(notNullize(SHELVED_RECYCLED_CHANGELIST_KEY.getData(dataContext)));
    shelvedChangeLists.addAll(notNullize(SHELVED_DELETED_CHANGELIST_KEY.getData(dataContext)));
    return shelvedChangeLists;
  }

  @NotNull
  public static List<ShelvedChangeList> getExactlySelectedLists(@NotNull final DataContext dataContext) {
    ChangesTree shelvedChangeTree = dataContext.getData(SHELVED_CHANGES_TREE);
    if (shelvedChangeTree == null) return emptyList();
    return StreamEx.of(VcsTreeModelData.exactlySelected(shelvedChangeTree).userObjectsStream(ShelvedChangeList.class)).toList();
  }

  @NotNull
  public static List<ShelvedChange> getShelveChanges(@NotNull final DataContext dataContext) {
    return notNullize(dataContext.getData(SHELVED_CHANGE_KEY));
  }

  @NotNull
  public static List<ShelvedBinaryFile> getBinaryShelveChanges(@NotNull final DataContext dataContext) {
    return notNullize(dataContext.getData(SHELVED_BINARY_FILE_KEY));
  }

  @NotNull
  public static List<String> getSelectedShelvedChangeNames(@NotNull final DataContext dataContext) {
    ChangesTree shelvedChangeTree = dataContext.getData(SHELVED_CHANGES_TREE);
    if (shelvedChangeTree == null) return emptyList();
    return StreamEx.of(VcsTreeModelData.selected(shelvedChangeTree).userObjectsStream(ShelvedWrapper.class))
      .map(ShelvedWrapper::getPath).toList();
  }

  private static final class MyShelveDeleteProvider implements DeleteProvider {
    @NotNull private final Project myProject;
    @NotNull private final ShelfTree myTree;

    private MyShelveDeleteProvider(@NotNull Project project, @NotNull ShelfTree tree) {
      myProject = project;
      myTree = tree;
    }

    @Override
    public void deleteElement(@NotNull DataContext dataContext) {
      List<ShelvedChangeList> shelvedListsToDelete = TreeUtil.collectSelectedObjectsOfType(myTree, ShelvedChangeList.class);

      List<ShelvedChange> changesToDelete = getChangesNotInLists(shelvedListsToDelete, getShelveChanges(dataContext));
      List<ShelvedBinaryFile> binariesToDelete = getBinariesNotInLists(shelvedListsToDelete, getBinaryShelveChanges(dataContext));

      ShelveChangesManager manager = ShelveChangesManager.getInstance(myProject);
      int fileListSize = binariesToDelete.size() + changesToDelete.size();
      Map<ShelvedChangeList, Date> createdDeletedListsWithOriginalDates =
        manager.deleteShelves(shelvedListsToDelete, getShelvedLists(dataContext), changesToDelete, binariesToDelete);
      if (!createdDeletedListsWithOriginalDates.isEmpty()) {
        showUndoDeleteNotification(shelvedListsToDelete, fileListSize, createdDeletedListsWithOriginalDates);
      }
    }

    private void showUndoDeleteNotification(@NotNull List<ShelvedChangeList> shelvedListsToDelete,
                                            int fileListSize,
                                            @NotNull Map<ShelvedChangeList, Date> createdDeletedListsWithOriginalDate) {
      String message = constructDeleteSuccessfullyMessage(fileListSize, shelvedListsToDelete.size(), getFirstItem(shelvedListsToDelete));
      Notification shelfDeletionNotification = new ShelfDeleteNotification(message);
      shelfDeletionNotification.addAction(new UndoShelfDeletionAction(myProject, createdDeletedListsWithOriginalDate));
      shelfDeletionNotification.addAction(ActionManager.getInstance().getAction("ShelvedChanges.ShowRecentlyDeleted"));
      VcsNotifier.getInstance(myProject).showNotificationAndHideExisting(shelfDeletionNotification, ShelfDeleteNotification.class);
    }

    private static final class UndoShelfDeletionAction extends NotificationAction {
      @NotNull private final Project myProject;
      @NotNull private final Map<ShelvedChangeList, Date> myListDateMap;

      private UndoShelfDeletionAction(@NotNull Project project, @NotNull Map<ShelvedChangeList, Date> listDateMap) {
        super(IdeBundle.messagePointer("undo.dialog.title"));
        myProject = project;
        myListDateMap = listDateMap;
      }

      @Override
      public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
        ShelveChangesManager manager = ShelveChangesManager.getInstance(myProject);
        List<ShelvedChangeList> cantRestoreList = findAll(myListDateMap.keySet(), l -> !manager.getDeletedLists().contains(l));
        myListDateMap.forEach((l, d) -> manager.restoreList(l, d));
        notification.expire();
        if (!cantRestoreList.isEmpty()) {
          VcsNotifier.getInstance(myProject).notifyMinorWarning(SHELVE_DELETION_UNDO,
                                                                VcsBundle.message("shelve.undo.deletion"),
                                                                VcsBundle.message("shelve.changes.restore.error", cantRestoreList.size()));
        }
      }
    }

    private static List<ShelvedBinaryFile> getBinariesNotInLists(@NotNull List<ShelvedChangeList> listsToDelete,
                                                                 @NotNull List<ShelvedBinaryFile> binaryFiles) {
      List<ShelvedBinaryFile> result = new ArrayList<>(binaryFiles);
      for (ShelvedChangeList list : listsToDelete) {
        result.removeAll(list.getBinaryFiles());
      }
      return result;
    }

    @NotNull
    private static List<ShelvedChange> getChangesNotInLists(@NotNull List<ShelvedChangeList> listsToDelete,
                                                            @NotNull List<ShelvedChange> shelvedChanges) {
      List<ShelvedChange> result = new ArrayList<>(shelvedChanges);
      // all changes should be loaded because action performed from loaded shelf tab
      listsToDelete.stream().map(list -> requireNonNull(list.getChanges())).forEach(result::removeAll);
      return result;
    }

    @NotNull
    private static String constructDeleteSuccessfullyMessage(int fileNum, int listNum, @Nullable ShelvedChangeList first) {
      String filesMessage = fileNum != 0 ? VcsBundle.message("shelve.delete.files.successful.message", fileNum) : "";
      String changelistsMessage = listNum != 0 ? VcsBundle
        .message("shelve.delete.changelists.message", listNum, listNum == 1 && first != null ? first.DESCRIPTION : "") : "";
      return StringUtil.capitalize(
        VcsBundle.message("shelve.delete.successful.message", filesMessage, fileNum > 0 && listNum > 0 ? 1 : 0, changelistsMessage));
    }

    @Override
    public boolean canDeleteElement(@NotNull DataContext dataContext) {
      return !getShelvedLists(dataContext).isEmpty();
    }
  }

  private static final class MyDnDTarget extends VcsToolwindowDnDTarget {
    private MyDnDTarget(@NotNull Project project, @NotNull Content content) {
      super(project, content);
    }

    @Override
    public void drop(DnDEvent event) {
      super.drop(event);
      Object attachedObject = event.getAttachedObject();
      if (attachedObject instanceof ChangeListDragBean) {
        FileDocumentManager.getInstance().saveAllDocuments();
        List<Change> changes = Arrays.asList(((ChangeListDragBean)attachedObject).getChanges());
        ShelveChangesManager.getInstance(myProject).shelveSilentlyUnderProgress(changes, true);
      }
    }

    @Override
    public boolean isDropPossible(@NotNull DnDEvent event) {
      Object attachedObject = event.getAttachedObject();
      return attachedObject instanceof ChangeListDragBean && ((ChangeListDragBean)attachedObject).getChanges().length > 0;
    }
  }

  private static final class ShelfToolWindowPanel implements ChangesViewContentManagerListener, DataProvider, Disposable {
    @NotNull private static final RegistryValue isOpenEditorDiffPreviewWithSingleClick =
      Registry.get("show.diff.preview.as.editor.tab.with.single.click");

    private final Project myProject;
    private final ShelveChangesManager myShelveChangesManager;
    private final VcsConfiguration myVcsConfiguration;

    @NotNull private final JScrollPane myTreeScrollPane;
    private final ShelfTree myTree;
    private final ActionToolbar myToolbar;
    @NotNull private final JPanel myRootPanel = new JPanel(new BorderLayout());

    private MyShelvedPreviewProcessor myChangeProcessor;
    private DiffPreview myDiffPreview;

    private ShelfToolWindowPanel(@NotNull Project project) {
      myProject = project;
      myShelveChangesManager = ShelveChangesManager.getInstance(myProject);
      myVcsConfiguration = VcsConfiguration.getInstance(myProject);

      myTree = new ShelfTree(myProject);
      myTree.setEditable(true);
      myTree.setDragEnabled(!ApplicationManager.getApplication().isHeadlessEnvironment());
      myTree.getGroupingSupport().setGroupingKeysOrSkip(myShelveChangesManager.getGrouping());
      myTree.addGroupingChangeListener(e -> {
        myShelveChangesManager.setGrouping(myTree.getGroupingSupport().getGroupingKeys());
        myTree.rebuildTree();
      });
      DefaultTreeCellEditor treeCellEditor = new DefaultTreeCellEditor(myTree, null) {
        @Override
        public boolean isCellEditable(EventObject event) {
          return !(event instanceof MouseEvent) && super.isCellEditable(event);
        }
      };
      myTree.setCellEditor(treeCellEditor);
      treeCellEditor.addCellEditorListener(new CellEditorListener() {
        @Override
        public void editingStopped(ChangeEvent e) {
          DefaultMutableTreeNode node = (DefaultMutableTreeNode)myTree.getLastSelectedPathComponent();
          if (node instanceof ShelvedListNode && e.getSource() instanceof TreeCellEditor) {
            String editorValue = ((TreeCellEditor)e.getSource()).getCellEditorValue().toString();
            ShelvedChangeList shelvedChangeList = ((ShelvedListNode)node).getList();
            myShelveChangesManager.renameChangeList(shelvedChangeList, editorValue);
          }
        }

        @Override
        public void editingCanceled(ChangeEvent e) {
        }
      });

      final AnAction showDiffAction = ActionManager.getInstance().getAction(IdeActions.ACTION_SHOW_DIFF_COMMON);
      showDiffAction.registerCustomShortcutSet(showDiffAction.getShortcutSet(), myTree);
      final EditSourceAction editSourceAction = new EditSourceAction();
      editSourceAction.registerCustomShortcutSet(editSourceAction.getShortcutSet(), myTree);

      DefaultActionGroup actionGroup = new DefaultActionGroup();
      actionGroup.addAll((ActionGroup)ActionManager.getInstance().getAction("ShelvedChangesToolbar"));
      actionGroup.add(Separator.getInstance());
      actionGroup.add(new MyToggleDetailsAction());

      myToolbar = ActionManager.getInstance().createActionToolbar("ShelvedChanges", actionGroup, false);
      myToolbar.setTargetComponent(myTree);
      myTreeScrollPane = ScrollPaneFactory.createScrollPane(myTree, SideBorder.LEFT);
      myRootPanel.add(myTreeScrollPane, BorderLayout.CENTER);
      addToolbar(isCommitToolWindowShown(myProject));
      setDiffPreview();
      EditorTabDiffPreviewManager.getInstance(project).subscribeToPreviewVisibilityChange(this, this::setDiffPreview);
      isOpenEditorDiffPreviewWithSingleClick.addListener(new RegistryValueListener() {
        @Override
        public void afterValueChanged(@NotNull RegistryValue value) {
          if (!isSplitterPreview()) setDiffPreview(true);
        }
      }, this);
      myProject.getMessageBus().connect(this).subscribe(ChangesViewContentManagerListener.TOPIC, this);

      DataManager.registerDataProvider(myRootPanel, this);

      PopupHandler.installPopupHandler(myTree, "ShelvedChangesPopupMenu", SHELF_CONTEXT_MENU);
    }

    @Override
    public void dispose() {
    }

    @Override
    public void toolWindowMappingChanged() {
      addToolbar(isCommitToolWindowShown(myProject));
    }

    private void addToolbar(boolean isHorizontal) {
      if (isHorizontal) {
        myToolbar.setOrientation(SwingConstants.HORIZONTAL);
        myRootPanel.add(myToolbar.getComponent(), BorderLayout.NORTH);
      }
      else {
        myToolbar.setOrientation(SwingConstants.VERTICAL);
        myRootPanel.add(myToolbar.getComponent(), BorderLayout.WEST);
      }
    }

    private void setDiffPreview() {
      setDiffPreview(false);
    }

    private void setDiffPreview(boolean force) {
      boolean isEditorPreview = EditorTabDiffPreviewManager.getInstance(myProject).isEditorDiffPreviewAvailable();
      if (!force) {
        if (isEditorPreview && myDiffPreview instanceof EditorTabPreview) return;
        if (!isEditorPreview && isSplitterPreview()) return;
      }

      if (myChangeProcessor != null) Disposer.dispose(myChangeProcessor);

      myChangeProcessor = new MyShelvedPreviewProcessor(myProject, myTree);
      Disposer.register(this, myChangeProcessor);

      myDiffPreview = isEditorPreview ? installEditorPreview(myChangeProcessor) : installSplitterPreview(myChangeProcessor);
    }

    @NotNull
    private EditorTabPreview installEditorPreview(@NotNull MyShelvedPreviewProcessor changeProcessor) {
      EditorTabPreview editorPreview = new EditorTabPreview(changeProcessor) {
        @Override
        protected String getCurrentName() {
          ShelvedWrapper myCurrentShelvedElement = changeProcessor.myCurrentShelvedElement;
          return myCurrentShelvedElement != null
                 ? VcsBundle.message("shelve.editor.diff.preview.title", myCurrentShelvedElement.getPresentableName())
                 : VcsBundle.message("shelved.version.name");
        }

        @Override
        protected boolean hasContent() {
          return changeProcessor.myCurrentShelvedElement != null;
        }

        @Override
        protected boolean skipPreviewUpdate() {
          if (super.skipPreviewUpdate()) return true;
          if (!myTree.equals(IdeFocusManager.getInstance(myProject).getFocusOwner())) return true;
          if (!isEditorPreviewAllowed()) return true;

          return false;
        }
      };
      editorPreview.setEscapeHandler(() -> {
        editorPreview.closePreview();

        ToolWindow toolWindow = getToolWindowFor(myProject, SHELF);
        if (toolWindow != null) toolWindow.activate(null);
      });
      if (isOpenEditorDiffPreviewWithSingleClick.asBoolean()) {
        editorPreview.openWithSingleClick(myTree);
      }
      else {
        editorPreview.openWithDoubleClick(myTree);
      }
      editorPreview.installNextDiffActionOn(myTreeScrollPane);

      return editorPreview;
    }

    @NotNull
    private PreviewDiffSplitterComponent installSplitterPreview(@NotNull MyShelvedPreviewProcessor changeProcessor) {
      PreviewDiffSplitterComponent previewSplitter =
        new PreviewDiffSplitterComponent(changeProcessor, SHELVE_PREVIEW_SPLITTER_PROPORTION);
      previewSplitter.setFirstComponent(myTreeScrollPane);
      previewSplitter.setPreviewVisible(myVcsConfiguration.SHELVE_DETAILS_PREVIEW_SHOWN, false);

      myTree.addSelectionListener(() -> previewSplitter.updatePreview(false), changeProcessor);

      myRootPanel.add(previewSplitter, BorderLayout.CENTER);
      Disposer.register(changeProcessor, () -> {
        myRootPanel.remove(previewSplitter);
        myRootPanel.add(myTreeScrollPane, BorderLayout.CENTER);

        myRootPanel.revalidate();
        myRootPanel.repaint();
      });

      return previewSplitter;
    }

    private boolean isSplitterPreview() {
      return myDiffPreview instanceof PreviewDiffSplitterComponent;
    }

    private boolean isEditorPreviewAllowed() {
      return !isOpenEditorDiffPreviewWithSingleClick.asBoolean() || myVcsConfiguration.SHELVE_DETAILS_PREVIEW_SHOWN;
    }

    private void openEditorPreview(boolean focusEditor) {
      if (isSplitterPreview()) return;
      if (!isEditorPreviewAllowed()) return;

      ((EditorTabPreview)myDiffPreview).openPreview(focusEditor);
    }

    @Nullable
    private DnDDragStartBean createDragStartBean(@NotNull DnDActionInfo info) {
      if (info.isMove()) {
        DataContext dc = DataManager.getInstance().getDataContext(myTree);
        return new DnDDragStartBean(new ShelvedChangeListDragBean(getShelveChanges(dc), getBinaryShelveChanges(dc), getShelvedLists(dc)));
      }
      return null;
    }

    @NotNull
    private DnDImage createDraggedImage(@NotNull DnDActionInfo info) {
      String imageText = VcsBundle.message("unshelve.changes.action");
      Image image = DnDAwareTree.getDragImage(myTree, imageText, null).getFirst();
      return new DnDImage(image, new Point(-image.getWidth(null), -image.getHeight(null)));
    }

    @Override
    public @Nullable Object getData(@NotNull String dataId) {
      return EditorTabDiffPreviewManager.EDITOR_TAB_DIFF_PREVIEW.is(dataId)
             ? (myDiffPreview instanceof EditorTabPreview) ? myDiffPreview : null
             : myTree.getData(dataId);
    }

    private class MyToggleDetailsAction extends ShowDiffPreviewAction {
      @Override
      public void update(@NotNull AnActionEvent e) {
        super.update(e);
        e.getPresentation().setEnabledAndVisible(isSplitterPreview() || isOpenEditorDiffPreviewWithSingleClick.asBoolean());
      }

      @Override
      public void setSelected(@NotNull AnActionEvent e, boolean state) {
        myDiffPreview.setPreviewVisible(state, false);
        myVcsConfiguration.SHELVE_DETAILS_PREVIEW_SHOWN = state;
      }

      @Override
      public boolean isSelected(@NotNull AnActionEvent e) {
        return myVcsConfiguration.SHELVE_DETAILS_PREVIEW_SHOWN;
      }
    }
  }

  private static class MyShelvedPreviewProcessor extends CacheDiffRequestProcessor<ShelvedWrapper> implements DiffPreviewUpdateProcessor {
    @NotNull private final Project myProject;
    @NotNull private final ShelfTree myTree;

    @NotNull private final DiffShelvedChangesActionProvider.PatchesPreloader myPreloader;
    @Nullable private ShelvedWrapper myCurrentShelvedElement;

    MyShelvedPreviewProcessor(@NotNull Project project, @NotNull ShelfTree tree) {
      super(project);
      myProject = project;
      myTree = tree;
      myPreloader = new DiffShelvedChangesActionProvider.PatchesPreloader(project);
    }

    @NotNull
    @Override
    protected String getRequestName(@NotNull ShelvedWrapper provider) {
      return provider.getRequestName();
    }

    @Override
    protected ShelvedWrapper getCurrentRequestProvider() {
      return myCurrentShelvedElement;
    }

    @RequiresEdt
    @Override
    public void clear() {
      if (myCurrentShelvedElement != null) {
        myCurrentShelvedElement = null;
        updateRequest();
      }
      dropCaches();
    }

    @Override
    @RequiresEdt
    public void refresh(boolean fromModelRefresh) {
      DataContext dc = DataManager.getInstance().getDataContext(myTree);
      List<ShelvedChange> selectedChanges = getShelveChanges(dc);
      List<ShelvedBinaryFile> selectedBinaryChanges = getBinaryShelveChanges(dc);

      if (selectedChanges.isEmpty() && selectedBinaryChanges.isEmpty()) {
        clear();
        return;
      }

      if (myCurrentShelvedElement != null) {
        if (keepBinarySelection(selectedBinaryChanges, myCurrentShelvedElement.getBinaryFile()) ||
            keepShelvedSelection(selectedChanges, myCurrentShelvedElement.getShelvedChange())) {
          dropCachesIfNeededAndUpdate(myCurrentShelvedElement);
          return;
        }
      }
      //getFirstSelected
      myCurrentShelvedElement = !selectedChanges.isEmpty()
                                ? new ShelvedWrapper(selectedChanges.get(0))
                                : new ShelvedWrapper(selectedBinaryChanges.get(0));
      dropCachesIfNeededAndUpdate(myCurrentShelvedElement);
    }

    private void dropCachesIfNeededAndUpdate(@NotNull ShelvedWrapper currentShelvedElement) {
      ShelvedChange shelvedChange = currentShelvedElement.getShelvedChange();
      boolean dropCaches = shelvedChange != null && myPreloader.isPatchFileChanged(shelvedChange.getPatchPath());
      if (dropCaches) {
        dropCaches();
      }
      updateRequest(dropCaches);
    }

    boolean keepShelvedSelection(@NotNull List<ShelvedChange> selectedChanges, @Nullable ShelvedChange currentShelvedChange) {
      return currentShelvedChange != null && selectedChanges.contains(currentShelvedChange);
    }

    boolean keepBinarySelection(@NotNull List<ShelvedBinaryFile> selectedBinaryChanges, @Nullable ShelvedBinaryFile currentBinary) {
      return currentBinary != null && selectedBinaryChanges.contains(currentBinary);
    }

    @NotNull
    @Override
    protected DiffRequest loadRequest(@NotNull ShelvedWrapper provider, @NotNull ProgressIndicator indicator)
      throws ProcessCanceledException, DiffRequestProducerException {
      String title = getRequestName(provider);
      try {
        ShelvedChange shelvedChange = provider.getShelvedChange();
        if (shelvedChange != null) {
          return createTextShelveRequest(shelvedChange, title);
        }

        ShelvedBinaryFile binaryFile = provider.getBinaryFile();
        if (binaryFile != null) {
          return createBinaryShelveRequest(binaryFile, title);
        }

        throw new IllegalStateException("Empty shelved wrapper: " + provider);
      }
      catch (VcsException | IOException e) {
        throw new DiffRequestProducerException(VcsBundle.message("changes.error.can.t.show.diff.for", title), e);
      }
    }

    @NotNull
    private DiffRequest createTextShelveRequest(@NotNull ShelvedChange shelvedChange, @Nullable @Nls String title)
      throws VcsException {
      DiffContentFactoryEx factory = DiffContentFactoryEx.getInstanceEx();
      Pair<TextFilePatch, CommitContext> pair = myPreloader.getPatchWithContext(shelvedChange);
      TextFilePatch patch = pair.first;
      CommitContext commitContext = pair.second;

      FilePath contextFilePath = getContextFilePath(shelvedChange);

      if (patch.isDeletedFile() || patch.isNewFile()) {
        DiffContent shelfContent = factory.create(myProject, patch.getSingleHunkPatchText(), contextFilePath);
        DiffContent emptyContent = factory.createEmpty();

        DiffContent leftContent = patch.isDeletedFile() ? shelfContent : emptyContent;
        DiffContent rightContent = !patch.isDeletedFile() ? shelfContent : emptyContent;
        String leftTitle = DiffBundle.message("merge.version.title.base");
        String rightTitle = VcsBundle.message("shelve.shelved.version");
        return new SimpleDiffRequest(title, leftContent, rightContent, leftTitle, rightTitle);
      }

      String path = chooseNotNull(patch.getAfterName(), patch.getBeforeName());
      CharSequence baseContents = PatchEP.EP_NAME.findExtensionOrFail(BaseRevisionTextPatchEP.class)
        .provideContent(myProject, path, commitContext);
      if (baseContents != null) {
        String patchedContent = PlainSimplePatchApplier.apply(baseContents, patch.getHunks());
        if (patchedContent != null) {
          DiffContent leftContent = factory.create(myProject, baseContents.toString(), contextFilePath);
          DiffContent rightContent = factory.create(myProject, patchedContent, contextFilePath);

          String leftTitle = DiffBundle.message("merge.version.title.base");
          String rightTitle = VcsBundle.message("shelve.shelved.version");
          return new SimpleDiffRequest(title, leftContent, rightContent, leftTitle, rightTitle);
        }
      }

      return new PatchDiffRequest(createAppliedTextPatch(patch), title, null);
    }

    @NotNull
    private static FilePath getContextFilePath(@NotNull ShelvedChange shelvedChange) {
      Change change = shelvedChange.getChange();
      if (change.getType() == Change.Type.MOVED) {
        FilePath bPath = requireNonNull(ChangesUtil.getBeforePath(change));
        FilePath aPath = requireNonNull(ChangesUtil.getAfterPath(change));
        if (bPath.getVirtualFile() != null) return bPath;
        if (aPath.getVirtualFile() != null) return aPath;
        return bPath;
      }
      return ChangesUtil.getFilePath(change);
    }

    @NotNull
    private SimpleDiffRequest createBinaryShelveRequest(@NotNull ShelvedBinaryFile binaryFile, @Nullable @Nls String title)
      throws DiffRequestProducerException, VcsException, IOException {
      DiffContentFactory factory = DiffContentFactory.getInstance();
      if (binaryFile.AFTER_PATH == null) {
        throw new DiffRequestProducerException(VcsBundle.message("changes.error.content.for.0.was.removed", title));
      }

      byte[] binaryContent = binaryFile.createBinaryContentRevision(myProject).getBinaryContent();
      FileType fileType = VcsUtil.getFilePath(binaryFile.SHELVED_PATH).getFileType();
      DiffContent shelfContent = factory.createBinary(myProject, binaryContent, fileType, title);
      return new SimpleDiffRequest(title, factory.createEmpty(), shelfContent, null, null);
    }
  }

  private static class ShelvedListNode extends ChangesBrowserNode<ShelvedChangeList> {
    private static final Icon PatchIcon = PatchFileType.INSTANCE.getIcon();
    private static final Icon AppliedPatchIcon =
      new IconSizeWrapper(Patch_applied, Patch_applied.getIconWidth(), Patch_applied.getIconHeight()) {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
          GraphicsUtil.paintWithAlpha(g, 0.6f);
          super.paintIcon(c, g, x, y);
        }
      };
    private static final Icon DisabledToDeleteIcon = IconUtil.desaturate(AllIcons.Actions.GC);

    @NotNull private final ShelvedChangeList myList;

    ShelvedListNode(@NotNull ShelvedChangeList list) {
      super(list);
      myList = list;
    }

    @NotNull
    public ShelvedChangeList getList() {
      return myList;
    }

    @Override
    public void render(@NotNull ChangesBrowserNodeRenderer renderer, boolean selected, boolean expanded, boolean hasFocus) {
      String listName = myList.DESCRIPTION;
      if (StringUtil.isEmptyOrSpaces(listName)) listName = VcsBundle.message("changes.nodetitle.empty.changelist.name");
      if (myList.isRecycled() || myList.isDeleted()) {
        renderer.appendTextWithIssueLinks(listName, SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES);
        renderer.setIcon(myList.isMarkedToDelete() || myList.isDeleted() ? DisabledToDeleteIcon : AppliedPatchIcon);
      }
      else {
        renderer.appendTextWithIssueLinks(listName, SimpleTextAttributes.REGULAR_ATTRIBUTES);
        renderer.setIcon(PatchIcon);
      }
      appendCount(renderer);
      String date = DateFormatUtil.formatPrettyDateTime(myList.DATE);
      renderer.append(", " + date, SimpleTextAttributes.GRAYED_ATTRIBUTES);
    }

    @Override
    public @Nls String getTextPresentation() {
      return getUserObject().toString();
    }
  }

  private static class ShelvedChangeNode extends ChangesBrowserNode<ShelvedWrapper> implements Comparable<ShelvedChangeNode> {

    @NotNull private final ShelvedWrapper myShelvedChange;
    @NotNull private final FilePath myFilePath;
    @Nullable @Nls private final String myAdditionalText;

    protected ShelvedChangeNode(@NotNull ShelvedWrapper shelvedChange,
                                @NotNull FilePath filePath,
                                @Nullable @Nls String additionalText) {
      super(shelvedChange);
      myShelvedChange = shelvedChange;
      myFilePath = filePath;
      myAdditionalText = additionalText;
    }

    @Override
    public void render(@NotNull ChangesBrowserNodeRenderer renderer, boolean selected, boolean expanded, boolean hasFocus) {
      String path = myShelvedChange.getRequestName();
      String directory = StringUtil.defaultIfEmpty(PathUtil.getParentPath(path), VcsBundle.message("shelve.default.path.rendering"));
      String fileName = StringUtil.defaultIfEmpty(PathUtil.getFileName(path), path);

      renderer.append(fileName, new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, myShelvedChange.getFileStatus().getColor()));
      if (myAdditionalText != null) {
        renderer.append(spaceAndThinSpace() + myAdditionalText, SimpleTextAttributes.REGULAR_ATTRIBUTES);
      }
      if (renderer.isShowFlatten()) {
        renderer.append(spaceAndThinSpace() + FileUtil.toSystemDependentName(directory), SimpleTextAttributes.GRAYED_ATTRIBUTES);
      }
      renderer.setIcon(FileTypeManager.getInstance().getFileTypeByFileName(fileName).getIcon());
    }

    @Override
    public String getTextPresentation() {
      return PathUtil.getFileName(myShelvedChange.getRequestName());
    }

    @Override
    protected boolean isFile() {
      return true;
    }

    @Override
    public int compareTo(@NotNull ShelvedChangeNode o) {
      return compareFilePaths(myFilePath, o.myFilePath);
    }

    @Nullable
    @Override
    public Color getBackgroundColor(@NotNull Project project) {
      return getBackgroundColorFor(project, myFilePath);
    }
  }

  private class MyContentUpdater extends Update {
    MyContentUpdater() {
      super("ShelfContentUpdate");
    }

    @Override
    public void run() {
      updateTreeModel();
    }

    @Override
    public boolean canEat(Update update) {
      return true;
    }
  }

  public static class PostStartupActivity implements StartupActivity.Background {
    @Override
    public void runActivity(@NotNull Project project) {
      if (ApplicationManager.getApplication().isHeadlessEnvironment()) return;

      getInstance(project).scheduleContentUpdate();
    }
  }
}
