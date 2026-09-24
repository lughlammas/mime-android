package com.lughlammas.mime.mime

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(Parameterized::class)
class GoldenParityTest(private val caseId: String) {

    @Test
    fun golden() = runTest {
        val case = GoldenHarness.loadCase(caseId)
        GoldenHarness.runCase(this, case)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<String>> =
            GoldenHarness.listCaseIds().map { arrayOf(it) }
    }
}
