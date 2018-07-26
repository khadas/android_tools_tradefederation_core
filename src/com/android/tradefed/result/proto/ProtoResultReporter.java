/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tradefed.result.proto;

import com.android.tradefed.config.OptionClass;
import com.android.tradefed.invoker.IInvocationContext;
import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.metrics.proto.MetricMeasurement.Metric;
import com.android.tradefed.result.ILogSaverListener;
import com.android.tradefed.result.ITestInvocationListener;
import com.android.tradefed.result.LogFile;
import com.android.tradefed.result.TestDescription;
import com.android.tradefed.result.proto.LogFileProto.LogFileInfo;
import com.android.tradefed.result.proto.TestRecordProto.ChildReference;
import com.android.tradefed.result.proto.TestRecordProto.DebugInfo;
import com.android.tradefed.result.proto.TestRecordProto.TestRecord;
import com.android.tradefed.result.proto.TestRecordProto.TestStatus;
import com.android.tradefed.testtype.suite.ModuleDefinition;

import com.google.protobuf.Any;
import com.google.protobuf.Timestamp;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;

/**
 * Result reporter build a {@link TestRecord} protobuf with all the results inside. Should be
 * extended to handle what to do with the final proto in {@link #processFinalProto(TestRecord)}.
 */
@OptionClass(alias = "proto-reporter")
abstract class ProtoResultReporter implements ITestInvocationListener, ILogSaverListener {

    private Stack<TestRecord.Builder> mLatestChild;
    private TestRecord.Builder mInvocationRecordBuilder;
    private long mInvocationStartTime;

    /**
     * Handling of the partial invocation test record proto after {@link
     * #invocationStarted(IInvocationContext)} occurred.
     *
     * @param invocationStartRecord The partial proto populated after the invocationStart.
     */
    public void processStartInvocation(TestRecord invocationStartRecord) {}

    /**
     * Handling of the final proto with all results.
     *
     * @param finalRecord The finalized proto with all the invocation results.
     */
    public void processFinalProto(TestRecord finalRecord) {}

    /**
     * Handling of the partial module record proto after {@link
     * #testModuleStarted(IInvocationContext)} occurred.
     *
     * @param moduleStartRecord The partial proto representing the module.
     */
    public void processTestModuleStarted(TestRecord moduleStartRecord) {}

    /**
     * Handling of the finalized module record proto after {@link #testModuleEnded()} occurred.
     *
     * @param moduleRecord The finalized proto representing the module.
     */
    public void processTestModuleEnd(TestRecord moduleRecord) {}

    /**
     * Handling of the partial test run record proto after {@link #testRunStarted(String, int)}
     * occurred.
     *
     * @param runStartedRecord The partial proto representing the run.
     */
    public void processTestRunStarted(TestRecord runStartedRecord) {}

    /**
     * Handling of the finalized run record proto after {@link #testRunEnded(long, HashMap)}
     * occurred.
     *
     * @param runRecord The finalized proto representing the run.
     */
    public void processTestRunEnded(TestRecord runRecord) {}

    /**
     * Handling of the partial test case record proto after {@link #testStarted(TestDescription,
     * long)} occurred.
     *
     * @param testCaseStartedRecord The partial proto representing the test case.
     */
    public void processTestCaseStarted(TestRecord testCaseStartedRecord) {}

    /**
     * Handling of the finalized test case record proto after {@link #testEnded(TestDescription,
     * long, HashMap)} occurred.
     *
     * @param testCaseRecord The finalized proto representing a test case.
     */
    public void processTestCaseEnded(TestRecord testCaseRecord) {}

    // Invocation events

    @Override
    public final void invocationStarted(IInvocationContext context) {
        mLatestChild = new Stack<>();
        mInvocationRecordBuilder = TestRecord.newBuilder();
        // Set invocation unique id
        mInvocationRecordBuilder.setTestRecordId(UUID.randomUUID().toString());

        // Populate start time of invocation
        mInvocationStartTime = System.currentTimeMillis();
        Timestamp startTime = createTimeStamp(mInvocationStartTime);
        mInvocationRecordBuilder.setStartTime(startTime);
        mInvocationRecordBuilder.setDescription(Any.pack(context.toProto()));

        // Put the invocation record at the bottom of the stack
        mLatestChild.add(mInvocationRecordBuilder);

        // Send the invocation proto with the currently set information to indicate the beginning
        // of the invocation.
        TestRecord startInvocationProto = mInvocationRecordBuilder.build();
        try {
            processStartInvocation(startInvocationProto);
        } catch (RuntimeException e) {
            CLog.e("Failed to process invocation started:");
            CLog.e(e);
        }
    }

    @Override
    public final void invocationEnded(long elapsedTime) {
        // Populate end time of invocation
        Timestamp endTime = createTimeStamp(mInvocationStartTime + elapsedTime);
        mInvocationRecordBuilder.setEndTime(endTime);

        // Finalize the protobuf handling: where to put the results.
        TestRecord record = mInvocationRecordBuilder.build();
        try {
            processFinalProto(record);
        } catch (RuntimeException e) {
            CLog.e("Failed to process invocation ended:");
            CLog.e(e);
        }
    }

    // Module events (optional when there is no suite)

    @Override
    public final void testModuleStarted(IInvocationContext moduleContext) {
        TestRecord.Builder moduleBuilder = TestRecord.newBuilder();
        moduleBuilder.setParentTestRecordId(mInvocationRecordBuilder.getTestRecordId());
        moduleBuilder.setTestRecordId(
                moduleContext.getAttributes().get(ModuleDefinition.MODULE_ID).get(0));
        moduleBuilder.setStartTime(createTimeStamp(System.currentTimeMillis()));
        moduleBuilder.setDescription(Any.pack(moduleContext.toProto()));
        mLatestChild.add(moduleBuilder);
        try {
            processTestModuleStarted(moduleBuilder.build());
        } catch (RuntimeException e) {
            CLog.e("Failed to process invocation ended:");
            CLog.e(e);
        }
    }

    @Override
    public final void testModuleEnded() {
        TestRecord.Builder moduleBuilder = mLatestChild.pop();
        moduleBuilder.setEndTime(createTimeStamp(System.currentTimeMillis()));
        TestRecord.Builder parentBuilder = mLatestChild.peek();

        // Finalize the module and track it in the child
        TestRecord moduleRecord = moduleBuilder.build();
        parentBuilder.addChildren(createChildReference(moduleRecord));
        try {
            processTestModuleEnd(moduleRecord);
        } catch (RuntimeException e) {
            CLog.e("Failed to process test module end:");
            CLog.e(e);
        }
    }

    // Run events

    @Override
    public final void testRunStarted(String runName, int testCount) {
        TestRecord.Builder runBuilder = TestRecord.newBuilder();
        TestRecord.Builder parent = mLatestChild.peek();
        runBuilder.setParentTestRecordId(parent.getTestRecordId());
        runBuilder.setTestRecordId(runName);
        runBuilder.setNumExpectedChildren(testCount);
        runBuilder.setStartTime(createTimeStamp(System.currentTimeMillis()));

        mLatestChild.add(runBuilder);
        try {
            processTestRunStarted(runBuilder.build());
        } catch (RuntimeException e) {
            CLog.e("Failed to process invocation ended:");
            CLog.e(e);
        }
    }

    @Override
    public final void testRunFailed(String errorMessage) {
        TestRecord.Builder current = mLatestChild.peek();
        DebugInfo.Builder debugBuilder = DebugInfo.newBuilder();
        debugBuilder.setErrorMessage(errorMessage);
        if (TestStatus.UNKNOWN.equals(current.getStatus())) {
            current.setDebugInfo(debugBuilder.build());
        } else {
            // We are in a test case and we need the run parent.
            TestRecord.Builder test = mLatestChild.pop();
            TestRecord.Builder run = mLatestChild.peek();
            run.setDebugInfo(debugBuilder.build());
            // Re-add the test
            mLatestChild.add(test);
        }
    }

    @Override
    public final void testRunEnded(long elapsedTimeMillis, HashMap<String, Metric> runMetrics) {
        TestRecord.Builder runBuilder = mLatestChild.pop();
        // TODO: Make sure the end time match the elapsed time
        runBuilder.setEndTime(createTimeStamp(System.currentTimeMillis()));
        runBuilder.putAllMetrics(runMetrics);
        TestRecord.Builder parentBuilder = mLatestChild.peek();

        // Finalize the run and track it in the child
        TestRecord runRecord = runBuilder.build();
        parentBuilder.addChildren(createChildReference(runRecord));
        try {
            processTestRunEnded(runRecord);
        } catch (RuntimeException e) {
            CLog.e("Failed to process test run end:");
            CLog.e(e);
        }
    }

    // test case events

    @Override
    public final void testStarted(TestDescription test, long startTime) {
        TestRecord.Builder testBuilder = TestRecord.newBuilder();
        TestRecord.Builder parent = mLatestChild.peek();
        testBuilder.setParentTestRecordId(parent.getTestRecordId());
        testBuilder.setTestRecordId(test.toString());
        testBuilder.setStartTime(createTimeStamp(startTime));
        testBuilder.setStatus(TestStatus.PASS);

        mLatestChild.add(testBuilder);
        try {
            processTestCaseStarted(testBuilder.build());
        } catch (RuntimeException e) {
            CLog.e("Failed to process invocation ended:");
            CLog.e(e);
        }
    }

    @Override
    public final void testEnded(
            TestDescription test, long endTime, HashMap<String, Metric> testMetrics) {
        TestRecord.Builder testBuilder = mLatestChild.pop();
        testBuilder.setEndTime(createTimeStamp(endTime));
        testBuilder.putAllMetrics(testMetrics);
        TestRecord.Builder parentBuilder = mLatestChild.peek();

        // Finalize the run and track it in the child
        TestRecord testCaseRecord = testBuilder.build();
        parentBuilder.addChildren(createChildReference(testCaseRecord));
        try {
            processTestCaseEnded(testCaseRecord);
        } catch (RuntimeException e) {
            CLog.e("Failed to process test case end:");
            CLog.e(e);
        }
    }

    @Override
    public final void testFailed(TestDescription test, String trace) {
        TestRecord.Builder testBuilder = mLatestChild.peek();

        testBuilder.setStatus(TestStatus.FAIL);
        DebugInfo.Builder debugBuilder = DebugInfo.newBuilder();
        // FIXME: extract the error message from the trace
        debugBuilder.setErrorMessage(trace);
        debugBuilder.setTrace(trace);
        testBuilder.setDebugInfo(debugBuilder.build());
    }

    @Override
    public final void testIgnored(TestDescription test) {
        TestRecord.Builder testBuilder = mLatestChild.peek();
        testBuilder.setStatus(TestStatus.IGNORED);
    }

    @Override
    public final void testAssumptionFailure(TestDescription test, String trace) {
        TestRecord.Builder testBuilder = mLatestChild.peek();

        testBuilder.setStatus(TestStatus.ASSUMPTION_FAILURE);
        DebugInfo.Builder debugBuilder = DebugInfo.newBuilder();
        // FIXME: extract the error message from the trace
        debugBuilder.setErrorMessage(trace);
        debugBuilder.setTrace(trace);
        testBuilder.setDebugInfo(debugBuilder.build());
    }

    // log events

    @Override
    public final void logAssociation(String dataName, LogFile logFile) {
        TestRecord.Builder current = mLatestChild.peek();
        Map<String, Any> fullmap = new HashMap<>();
        fullmap.putAll(current.getArtifacts());
        Any any = Any.pack(createFileProto(logFile));
        fullmap.put(dataName, any);
        current.putAllArtifacts(fullmap);
    }

    private ChildReference createChildReference(TestRecord record) {
        ChildReference.Builder child = ChildReference.newBuilder();
        child.setTestRecordId(record.getTestRecordId());
        child.setInlineTestRecord(record);
        return child.build();
    }

    /** Create and populate Timestamp as recommended in the javadoc of the Timestamp proto. */
    private Timestamp createTimeStamp(long currentTimeMs) {
        return Timestamp.newBuilder()
                .setSeconds(currentTimeMs / 1000)
                .setNanos((int) ((currentTimeMs % 1000) * 1000000))
                .build();
    }

    private LogFileInfo createFileProto(LogFile logFile) {
        LogFileInfo.Builder logFileBuilder = LogFileInfo.newBuilder();
        logFileBuilder
                .setPath(logFile.getPath())
                .setUrl(logFile.getUrl())
                .setIsText(logFile.isText())
                .setLogType(logFile.getType().toString())
                .setIsCompressed(logFile.isCompressed())
                .setSize(logFile.getSize());
        return logFileBuilder.build();
    }
}