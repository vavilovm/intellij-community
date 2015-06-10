/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.refactoring.rename;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners;
import org.jetbrains.kotlin.test.JetTestUtils;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.generators.tests.TestsPackage}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("idea/testData/refactoring/rename")
@TestDataPath("$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
public class RenameTestGenerated extends AbstractRenameTest {
    public void testAllFilesPresentInRename() throws Exception {
        JetTestUtils.assertAllTestsPresentInSingleGeneratedClass(this.getClass(), new File("idea/testData/refactoring/rename"), Pattern.compile("^(.+)\\.test$"));
    }

    @TestMetadata("automaticRenamer/simple.test")
    public void testAutomaticRenamer_Simple() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/automaticRenamer/simple.test");
        doTest(fileName);
    }

    @TestMetadata("automaticRenamerForJavaClass/javaClass.test")
    public void testAutomaticRenamerForJavaClass_JavaClass() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/automaticRenamerForJavaClass/javaClass.test");
        doTest(fileName);
    }

    @TestMetadata("automaticRenamerJavaParameter/parameter.test")
    public void testAutomaticRenamerJavaParameter_Parameter() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/automaticRenamerJavaParameter/parameter.test");
        doTest(fileName);
    }

    @TestMetadata("automaticRenamerOverloads/package.test")
    public void testAutomaticRenamerOverloads_Package() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/automaticRenamerOverloads/package.test");
        doTest(fileName);
    }

    @TestMetadata("automaticRenamerOverloadsClass/class.test")
    public void testAutomaticRenamerOverloadsClass_Class() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/automaticRenamerOverloadsClass/class.test");
        doTest(fileName);
    }

    @TestMetadata("automaticRenamerOverloadsJavaClass/overloads.test")
    public void testAutomaticRenamerOverloadsJavaClass_Overloads() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/automaticRenamerOverloadsJavaClass/overloads.test");
        doTest(fileName);
    }

    @TestMetadata("automaticRenamerOverloadsObject/object.test")
    public void testAutomaticRenamerOverloadsObject_Object() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/automaticRenamerOverloadsObject/object.test");
        doTest(fileName);
    }

    @TestMetadata("automaticRenamerParameter/parameter.test")
    public void testAutomaticRenamerParameter_Parameter() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/automaticRenamerParameter/parameter.test");
        doTest(fileName);
    }

    @TestMetadata("automaticRenamerParameterInExtension/parameter.test")
    public void testAutomaticRenamerParameterInExtension_Parameter() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/automaticRenamerParameterInExtension/parameter.test");
        doTest(fileName);
    }

    @TestMetadata("defaultObject/defaultObject.test")
    public void testDefaultObject_DefaultObject() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/defaultObject/defaultObject.test");
        doTest(fileName);
    }

    @TestMetadata("defaultObjectWithDefaultName/defaultObject.test")
    public void testDefaultObjectWithDefaultName_DefaultObject() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/defaultObjectWithDefaultName/defaultObject.test");
        doTest(fileName);
    }

    @TestMetadata("renameArgumentsWhenParameterRenamed/parameter.test")
    public void testRenameArgumentsWhenParameterRenamed_Parameter() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameArgumentsWhenParameterRenamed/parameter.test");
        doTest(fileName);
    }

    @TestMetadata("renameCompareTo/compareTo.test")
    public void testRenameCompareTo_CompareTo() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameCompareTo/compareTo.test");
        doTest(fileName);
    }

    @TestMetadata("renameContains/contains.test")
    public void testRenameContains_Contains() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameContains/contains.test");
        doTest(fileName);
    }

    @TestMetadata("renameContainsWithConflicts/containsWithConflicts.test")
    public void testRenameContainsWithConflicts_ContainsWithConflicts() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameContainsWithConflicts/containsWithConflicts.test");
        doTest(fileName);
    }

    @TestMetadata("renameEquals/equals.test")
    public void testRenameEquals_Equals() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameEquals/equals.test");
        doTest(fileName);
    }

    @TestMetadata("renameExplicitComponentFunction/explicitComponentFunction.test")
    public void testRenameExplicitComponentFunction_ExplicitComponentFunction() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameExplicitComponentFunction/explicitComponentFunction.test");
        doTest(fileName);
    }

    @TestMetadata("renameGet/get.test")
    public void testRenameGet_Get() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameGet/get.test");
        doTest(fileName);
    }

    @TestMetadata("renameInc/inc.test")
    public void testRenameInc_Inc() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameInc/inc.test");
        doTest(fileName);
    }

    @TestMetadata("renameInvoke/invoke.test")
    public void testRenameInvoke_Invoke() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameInvoke/invoke.test");
        doTest(fileName);
    }

    @TestMetadata("renameIterator/iterator.test")
    public void testRenameIterator_Iterator() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameIterator/iterator.test");
        doTest(fileName);
    }

    @TestMetadata("renameJavaClass/renameJavaClass.test")
    public void testRenameJavaClass_RenameJavaClass() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameJavaClass/renameJavaClass.test");
        doTest(fileName);
    }

    @TestMetadata("renameJavaInterface/renameJavaInterface.test")
    public void testRenameJavaInterface_RenameJavaInterface() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameJavaInterface/renameJavaInterface.test");
        doTest(fileName);
    }

    @TestMetadata("renameJavaMethod/javaBaseMethod.test")
    public void testRenameJavaMethod_JavaBaseMethod() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameJavaMethod/javaBaseMethod.test");
        doTest(fileName);
    }

    @TestMetadata("renameJavaMethod/kotlinOverridenMethod.test")
    public void testRenameJavaMethod_KotlinOverridenMethod() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameJavaMethod/kotlinOverridenMethod.test");
        doTest(fileName);
    }

    @TestMetadata("renameKotlinBaseMethod/javaWrapperForBaseFunction.test")
    public void testRenameKotlinBaseMethod_JavaWrapperForBaseFunction() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameKotlinBaseMethod/javaWrapperForBaseFunction.test");
        doTest(fileName);
    }

    @TestMetadata("renameKotlinBaseMethod/javaWrapperForOverridenFunctionWithKotlinBase.test")
    public void testRenameKotlinBaseMethod_JavaWrapperForOverridenFunctionWithKotlinBase() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameKotlinBaseMethod/javaWrapperForOverridenFunctionWithKotlinBase.test");
        doTest(fileName);
    }

    @TestMetadata("renameKotlinBaseMethod/kotlinBaseFunction.test")
    public void testRenameKotlinBaseMethod_KotlinBaseFunction() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameKotlinBaseMethod/kotlinBaseFunction.test");
        doTest(fileName);
    }

    @TestMetadata("renameKotlinClass/javaWrapperForKotlinClass.test")
    public void testRenameKotlinClass_JavaWrapperForKotlinClass() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameKotlinClass/javaWrapperForKotlinClass.test");
        doTest(fileName);
    }

    @TestMetadata("renameKotlinClass/kotlinClass.test")
    public void testRenameKotlinClass_KotlinClass() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameKotlinClass/kotlinClass.test");
        doTest(fileName);
    }

    @TestMetadata("renameKotlinClassConstructor/renameKotlinConstructor.test")
    public void testRenameKotlinClassConstructor_RenameKotlinConstructor() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameKotlinClassConstructor/renameKotlinConstructor.test");
        doTest(fileName);
    }

    @TestMetadata("renameKotlinClassSecondaryConstructor/renameKotlinSecondaryConstructor.test")
    public void testRenameKotlinClassSecondaryConstructor_RenameKotlinSecondaryConstructor() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameKotlinClassSecondaryConstructor/renameKotlinSecondaryConstructor.test");
        doTest(fileName);
    }

    @TestMetadata("renameKotlinClassWithFile/javaClassWrapper.test")
    public void testRenameKotlinClassWithFile_JavaClassWrapper() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameKotlinClassWithFile/javaClassWrapper.test");
        doTest(fileName);
    }

    @TestMetadata("renameKotlinClassWithFile/kotlinClass.test")
    public void testRenameKotlinClassWithFile_KotlinClass() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameKotlinClassWithFile/kotlinClass.test");
        doTest(fileName);
    }

    @TestMetadata("renameKotlinEnum/renameKotlinEnum.test")
    public void testRenameKotlinEnum_RenameKotlinEnum() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameKotlinEnum/renameKotlinEnum.test");
        doTest(fileName);
    }

    @TestMetadata("renameKotlinFunctionInEnum/renameKotlinFunctionInEnum.test")
    public void testRenameKotlinFunctionInEnum_RenameKotlinFunctionInEnum() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameKotlinFunctionInEnum/renameKotlinFunctionInEnum.test");
        doTest(fileName);
    }

    @TestMetadata("renameKotlinMethod/javaWrapperForKotlinMethod.test")
    public void testRenameKotlinMethod_JavaWrapperForKotlinMethod() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameKotlinMethod/javaWrapperForKotlinMethod.test");
        doTest(fileName);
    }

    @TestMetadata("renameKotlinMethod/renameKotlinMethod.test")
    public void testRenameKotlinMethod_RenameKotlinMethod() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameKotlinMethod/renameKotlinMethod.test");
        doTest(fileName);
    }

    @TestMetadata("renameKotlinPackage/renameInOtherFile.test")
    public void testRenameKotlinPackage_RenameInOtherFile() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameKotlinPackage/renameInOtherFile.test");
        doTest(fileName);
    }

    @TestMetadata("renameKotlinPackage/renameKotlinPackage.test")
    public void testRenameKotlinPackage_RenameKotlinPackage() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameKotlinPackage/renameKotlinPackage.test");
        doTest(fileName);
    }

    @TestMetadata("renameKotlinPackageClass/javaWrapperForKotlinPackageClass.test")
    public void testRenameKotlinPackageClass_JavaWrapperForKotlinPackageClass() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameKotlinPackageClass/javaWrapperForKotlinPackageClass.test");
        doTest(fileName);
    }

    @TestMetadata("renameKotlinPackageFunctionFromJava/renameKotlinPackageFunctionFromJava.test")
    public void testRenameKotlinPackageFunctionFromJava_RenameKotlinPackageFunctionFromJava() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameKotlinPackageFunctionFromJava/renameKotlinPackageFunctionFromJava.test");
        doTest(fileName);
    }

    @TestMetadata("renameKotlinValProperty/renameAsJavaGetterForExplicitGetter.test")
    public void testRenameKotlinValProperty_RenameAsJavaGetterForExplicitGetter() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameKotlinValProperty/renameAsJavaGetterForExplicitGetter.test");
        doTest(fileName);
    }

    @TestMetadata("renameKotlinValProperty/renameBase.test")
    public void testRenameKotlinValProperty_RenameBase() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameKotlinValProperty/renameBase.test");
        doTest(fileName);
    }

    @TestMetadata("renameKotlinValProperty/renameOverriden.test")
    public void testRenameKotlinValProperty_RenameOverriden() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameKotlinValProperty/renameOverriden.test");
        doTest(fileName);
    }

    @TestMetadata("renameKotlinVarProperty/renameAsJavaGetter.test")
    public void testRenameKotlinVarProperty_RenameAsJavaGetter() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameKotlinVarProperty/renameAsJavaGetter.test");
        doTest(fileName);
    }

    @TestMetadata("renameKotlinVarProperty/renameAsJavaSetter.test")
    public void testRenameKotlinVarProperty_RenameAsJavaSetter() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameKotlinVarProperty/renameAsJavaSetter.test");
        doTest(fileName);
    }

    @TestMetadata("renameKotlinVarProperty/renameBase.test")
    public void testRenameKotlinVarProperty_RenameBase() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameKotlinVarProperty/renameBase.test");
        doTest(fileName);
    }

    @TestMetadata("renameKotlinVarProperty/renameOverriden.test")
    public void testRenameKotlinVarProperty_RenameOverriden() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameKotlinVarProperty/renameOverriden.test");
        doTest(fileName);
    }

    @TestMetadata("renamePlus/plus.test")
    public void testRenamePlus_Plus() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renamePlus/plus.test");
        doTest(fileName);
    }

    @TestMetadata("renamePlusAssign/plusAssign.test")
    public void testRenamePlusAssign_PlusAssign() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renamePlusAssign/plusAssign.test");
        doTest(fileName);
    }

    @TestMetadata("renameSet/set.test")
    public void testRenameSet_Set() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameSet/set.test");
        doTest(fileName);
    }

    @TestMetadata("renameSynthesizedComponentFunction/synthesizedComponentFunction.test")
    public void testRenameSynthesizedComponentFunction_SynthesizedComponentFunction() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameSynthesizedComponentFunction/synthesizedComponentFunction.test");
        doTest(fileName);
    }

    @TestMetadata("renameUnaryMinus/unaryMinus.test")
    public void testRenameUnaryMinus_UnaryMinus() throws Exception {
        String fileName = JetTestUtils.navigationMetadata("idea/testData/refactoring/rename/renameUnaryMinus/unaryMinus.test");
        doTest(fileName);
    }
}
