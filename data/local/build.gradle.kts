plugins {
    id(Plugins.androidLibraryCommon)
    kotlin(Plugins.kapt)
}

dependencies {
    // ROOM
    kapt(Libraries.roomCompiler)
    implementation(project(Modules.common))
    implementation(Libraries.roomRunTime)
    implementation(Libraries.roomKtx)
    implementation(AndroidLibraries.lifecycleExtensions)
    // TEST MODULE
    //androidTestImplementation(project(Modules.commonTest))
    // DATA MODULE
    implementation(project(Modules.model))
    // KOIN
    implementation(Libraries.koin)
    implementation(Libraries.koinViewModel)
    // TEST
    androidTestImplementation(TestLibraries.androidTestRunner)
    androidTestImplementation(TestLibraries.junit)
    androidTestImplementation(TestLibraries.koin)
    androidTestImplementation(TestLibraries.archCoreTest)
}