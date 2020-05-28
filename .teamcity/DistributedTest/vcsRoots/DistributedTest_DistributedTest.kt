package DistributedTest.vcsRoots

import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot

object DistributedTest_DistributedTest : GitVcsRoot({
    uuid = "ab4bf3e6-1be4-4ce4-bc3d-6c4b39361eed"
    name = "DistributedTest"
    url = "https://github.com/gradle/gradle.git"
    branch = "refs/heads/blindpirate/distributed-test"
    authMethod = password {
        userName = "blindpirate"
        password = "credentialsJSON:9b51dc85-6a07-4208-9753-e02ed0cd6237"
    }
})
