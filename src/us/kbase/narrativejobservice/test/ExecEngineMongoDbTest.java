package us.kbase.narrativejobservice.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.mongodb.WriteConcernException;

import us.kbase.common.test.controllers.mongo.MongoController;
import us.kbase.narrativejobservice.db.ExecEngineMongoDb;
import us.kbase.narrativejobservice.db.ExecLog;
import us.kbase.narrativejobservice.db.ExecLogLine;
import us.kbase.narrativejobservice.db.ExecTask;

public class ExecEngineMongoDbTest {
    private static MongoController mongo;
    private static ExecEngineMongoDb db = null;
    
    @BeforeClass
    public static void startup() throws Exception {
        Properties props = TesterUtils.props();
        File workDir = TesterUtils.prepareWorkDir(new File("temp_files"),
                "awe-ee_mongodb");
        File mongoDir = new File(workDir, "mongo");
        String mongoExepath = TesterUtils.getMongoExePath(props);
        System.out.print("Starting MongoDB executable at " + mongoExepath +
                "... ");
        mongo = new MongoController(mongoExepath, mongoDir.toPath());
        System.out.println("Done. Port " + mongo.getServerPort());
        
        db = new ExecEngineMongoDb("localhost:" + mongo.getServerPort(),
                "exec_engine", null, null, null);
    }

    @AfterClass
    public static void shutdown() throws Exception {
        if (mongo != null)
            mongo.destroy(false);
    }
    
    @Test
    public void testSrvProps() throws Exception {
        String key1 = "prop1";
        String val1 = db.getServiceProperty(key1);
        Assert.assertNull(val1);
        val1 = "val1";
        db.setServiceProperty(key1, val1);
        Assert.assertEquals(val1, db.getServiceProperty(key1));
        val1 = "val1_";
        db.setServiceProperty(key1, val1);
        Assert.assertEquals(val1, db.getServiceProperty(key1));
        String key2 = "prop2";
        String val2 = "val2";
        db.setServiceProperty(key2, val2);
        Assert.assertEquals(val1, db.getServiceProperty(key1));
        Assert.assertEquals(val2, db.getServiceProperty(key2));        
    }
    
    @Test
    public void testTasks() throws Exception {
        String taskId1 = "task_1";
        ExecTask task1 = new ExecTask();
        task1.setUjsJobId(taskId1);
        task1.setAweJobId("123");
        task1.setInputShockId("isn1");
        task1.setOutputShockId("osn1");
        task1.setCreationTime(1L);
        db.insertExecTask(task1);
        try {
            db.insertExecTask(task1);
            Assert.fail();
        } catch (Exception ex) {
            Assert.assertTrue(ex.getMessage(), ex.getMessage().contains("duplicate key"));
        }
        String taskId2 = "task_2";
        ExecTask task2 = new ExecTask();
        task2.setUjsJobId(taskId2);
        task2.setAweJobId("456");
        task2.setInputShockId("isn2");
        task2.setOutputShockId("osn2");
        task2.setCreationTime(2L);
        db.insertExecTask(task2);
        db.updateExecTaskTime(taskId2, false, 3L);
        ExecTask task3 = db.getExecTask(taskId2);
        Assert.assertEquals(task2.getAweJobId(), task3.getAweJobId());
        Assert.assertEquals(2L, (long)task3.getCreationTime());
        Assert.assertEquals(3L, (long)task3.getExecStartTime());
        Assert.assertNull(task3.getFinishTime());
        db.updateExecTaskTime(taskId2, true, 4L);
        task3 = db.getExecTask(taskId2);
        Assert.assertEquals(2L, (long)task3.getCreationTime());
        Assert.assertEquals(3L, (long)task3.getExecStartTime());
        Assert.assertEquals(4L, (long)task3.getFinishTime());
    }
    
    @Test
    public void testLogs() throws Exception {
        String ujsJobId = "logs_1";
        ExecLog dbLog = new ExecLog();
        dbLog.setUjsJobId(ujsJobId);
        dbLog.setOriginalLineCount(0);
        dbLog.setStoredLineCount(0);
        dbLog.setLines(new ArrayList<ExecLogLine>());
        db.insertExecLog(dbLog);
        int count = 26;
        for (int i = 0; i < count; i++) {
            ExecLogLine ell = new ExecLogLine();
            ell.setLinePos(i);
            ell.setLine("" + (char)('a' + i));
            ell.setIsError(i % 2 == 1);
            List<ExecLogLine> part = Arrays.asList(ell);
            db.updateExecLogLines(ujsJobId, i + 1, part);
        }
        dbLog = db.getExecLog(ujsJobId);
        dbLog.setLines(db.getExecLogLines(ujsJobId, 0, 2 * count));
        Assert.assertEquals(ujsJobId, dbLog.getUjsJobId());
        Assert.assertEquals(count, (int)dbLog.getOriginalLineCount());
        Assert.assertEquals(count, (int)dbLog.getStoredLineCount());
        Assert.assertEquals(count, dbLog.getLines().size());
        for (int i = 0; i < count; i++) {
            ExecLogLine ell = dbLog.getLines().get(i);
            Assert.assertEquals(i, (int)ell.getLinePos());
            Assert.assertEquals("" + (char)('a' + i), ell.getLine());
            Assert.assertEquals(i % 2, ell.getIsError() ? 1 : 0);
        }
    }

    @Test
    public void testSpeedAndSize() throws Exception {
        int count = 400;
        int partSize = 100;
        int lineLen = 1000;
        char[] logChars = new char[lineLen];
        Arrays.fill(logChars, 'a');
        String logText = new String(logChars);
        int id = 1;
        long t1 = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            List<ExecLog> part = new ArrayList<ExecLog>();
            for (int j = 0; j < partSize; j++) {
                id++;
                String ujsJobId = "sas_" + id;
                ExecLog dbLog = new ExecLog();
                dbLog.setUjsJobId(ujsJobId);
                dbLog.setOriginalLineCount(0);
                dbLog.setStoredLineCount(0);
                ExecLogLine ell = new ExecLogLine();
                ell.setLinePos(0);
                ell.setLine(logText);
                ell.setIsError(false);
                dbLog.setLines(Arrays.asList(ell));
                part.add(dbLog);
            }
            db.insertExecLogs(part);
        }
        t1 = System.currentTimeMillis() - t1;
        System.out.println("testSpeedAndSize: t1=" + t1 + " (" + (t1 / (double)count) + " per insert)");
        count = 160;
        id++;
        String ujsJobId = "sas_" + id;
        ExecLog dbLog = new ExecLog();
        dbLog.setUjsJobId(ujsJobId);
        dbLog.setOriginalLineCount(0);
        dbLog.setStoredLineCount(0);
        dbLog.setLines(new ArrayList<ExecLogLine>());
        db.insertExecLog(dbLog);
        long t2 = System.currentTimeMillis();
        for (int i = 0; i < count + 1; i++) {
            List<ExecLogLine> part = new ArrayList<ExecLogLine>();
            for (int j = 0; j < partSize; j++) {
                ExecLogLine ell = new ExecLogLine();
                ell.setLinePos(i * partSize + j);
                ell.setLine(logText);
                ell.setIsError(false);
                part.add(ell);
            }
            try {
                db.updateExecLogLines(ujsJobId, i + 1, part);
                Assert.assertTrue(i != count);
            } catch (WriteConcernException ex) {
                Assert.assertEquals(count, i);
                Assert.assertTrue(ex.getMessage(), ex.getMessage().contains("Size must be between 0 and "));
                Assert.assertEquals(10334, ex.getCode());
            }
        }
        t2 = System.currentTimeMillis() - t2;
        System.out.println("testSpeedAndSize: t2=" + t2 + " (" + (t2 / (double)count) + " per insert)");
    }
}
