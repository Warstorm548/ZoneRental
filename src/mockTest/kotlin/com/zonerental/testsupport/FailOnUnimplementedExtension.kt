package com.zonerental.testsupport

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler
import org.mockbukkit.mockbukkit.exception.UnimplementedOperationException

/**
 * MockBukkit's UnimplementedOperationException aborts a test, which JUnit reports as *skipped*.
 * That would silently hide coverage gaps, so turn it into a real failure.
 * Only @Disabled known-issue tests should ever show up as skipped.
 */
class FailOnUnimplementedExtension : TestExecutionExceptionHandler, LifecycleMethodExecutionExceptionHandler {

    private fun convert(throwable: Throwable): Throwable =
        if (throwable is UnimplementedOperationException) {
            AssertionError("MockBukkit does not implement an API this test uses: ${throwable.stackTrace.firstOrNull()}", throwable)
        } else throwable

    override fun handleTestExecutionException(context: ExtensionContext, throwable: Throwable) = throw convert(throwable)
    override fun handleBeforeEachMethodExecutionException(context: ExtensionContext, throwable: Throwable) = throw convert(throwable)
    override fun handleAfterEachMethodExecutionException(context: ExtensionContext, throwable: Throwable) = throw convert(throwable)
}
