package com.sunnysuperman.fastsearch.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.FSLockFactory;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.NumericUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sunnysuperman.commons.model.Pagination;
import com.sunnysuperman.commons.util.FileUtil;
import com.sunnysuperman.commons.util.FormatUtil;
import com.sunnysuperman.commons.util.StringUtil;
import com.sunnysuperman.fastsearch.analysis.ComplexAnalyzer;
import com.sunnysuperman.fastsearch.commit.Commit;
import com.sunnysuperman.fastsearch.commit.CommitRepository;
import com.sunnysuperman.fastsearch.doc.DocRepository;
import com.sunnysuperman.fastsearch.doc.DocRoster;

public class Searcher {
    public static final String TOKEN = " ";
    private static final Logger LOG = LoggerFactory.getLogger(Searcher.class);
    private static final boolean VERBOSE = LOG.isInfoEnabled();
    private static final Analyzer analyzer = new ComplexAnalyzer();
    private final byte[] CLOSE_READER_LOCK = new byte[0];
    private final LinkedList<SearchReader> closingReaders = new LinkedList<SearchReader>();
    private final byte[] PATH_WRITE_LOCK = new byte[0];
    private final Timer indexTimer = new Timer();
    private final SearcherOptions options;
    private final File dir;
    private Timer fullIndexTimer;
    private volatile File stagePath;
    private volatile File dataPath;
    private volatile DirectoryReader directoryReader;
    private volatile boolean started = false;

    private void verbose(String s) {
        LOG.info("[" + options.getName() + "]: " + s);
    }

    private class SearchIndexTask extends TimerTask {

        private void doIndex() {
            if (VERBOSE) {
                verbose("Index work starts");
            }
            int commits = -1;
            boolean readerChanged = false;
            synchronized (PATH_WRITE_LOCK) {
                try {
                    commits = indexFromCommits(dataPath, -1);
                } catch (Throwable t) {
                    LOG.error(null, t);
                }
                if (commits > 0 && containsIndex(dataPath)) {
                    DirectoryReader newReader = openReader(dataPath);
                    if (newReader != null) {
                        setReader(newReader);
                        readerChanged = true;
                    }
                }
            }
            if (readerChanged) {
                testSearch();
            }
            if (VERBOSE) {
                verbose("Index work ends");
            }
        }

        @Override
        public void run() {
            try {
                doIndex();
            } catch (Throwable t) {
                LOG.error(null, t);
            }
            scheduleIndex();
        }
    };

    private class RemoveOldIndexTask extends TimerTask {

        @Override
        public void run() {
            if (VERBOSE) {
                verbose("RemoveOldIndexTask start");
            }
            synchronized (PATH_WRITE_LOCK) {
                try {
                    for (File sub : dir.listFiles()) {
                        if (Files.isSymbolicLink(sub.toPath())) {
                            if (VERBOSE) {
                                verbose("Skip current link");
                            }
                            continue;
                        }
                        String absolutePath = sub.getAbsolutePath();
                        if (dataPath != null && absolutePath.equals(dataPath.getAbsolutePath())) {
                            if (VERBOSE) {
                                verbose("Skip dataPath: " + dataPath.getAbsolutePath());
                            }
                            continue;
                        }
                        if (stagePath != null && absolutePath.equals(stagePath.getAbsolutePath())) {
                            if (VERBOSE) {
                                verbose("Skip stagePath: " + stagePath.getAbsolutePath());
                            }
                            continue;
                        }
                        FileUtil.delete(sub);
                    }
                } catch (Throwable t) {
                    LOG.error(null, t);
                }
            }
            if (VERBOSE) {
                verbose("RemoveOldIndexTask end");
            }
        }
    };

    private final class CloseReaderTask extends TimerTask {

        @Override
        public void run() {
            if (VERBOSE) {
                verbose("Close reader task start");
            }
            synchronized (CLOSE_READER_LOCK) {
                if (!closingReaders.isEmpty()) {
                    long expire = System.currentTimeMillis() - 30 * 1000;
                    for (Iterator<SearchReader> iter = closingReaders.iterator(); iter.hasNext();) {
                        SearchReader reader = iter.next();
                        if (reader.getQueueAt() < expire) {
                            iter.remove();
                            FileUtil.close(reader.getReader());
                        }
                    }
                }
            }
            if (VERBOSE) {
                verbose("Close reader task end");
            }
        }

    }

    private class FullIndexTask extends TimerTask {

        @Override
        public void run() {
            fullIndex();
            scheduleFullIndex();
        }

    }

    public Searcher(final SearcherOptions options) {
        super();
        this.options = options.froze();
        dir = new File(options.getDir());
    }

    private File ensureCommitMetaFile(File dir) throws IOException {
        File commitMarkFile = new File(dir, ".commit");
        FileUtil.ensureFile(commitMarkFile);
        return commitMarkFile;
    }

    private File ensureVersionMetaFile(File dir) throws IOException {
        File versionMarkFile = new File(dir, ".version");
        FileUtil.ensureFile(versionMarkFile);
        return versionMarkFile;
    }

    private long readCommitId(File path) throws IOException {
        String s = StringUtil.trimToNull(FileUtil.read(ensureCommitMetaFile(path)));
        return FormatUtil.parseLongValue(s, -1);
    }

    private void writeCommitId(File path, long commitId) throws IOException {
        FileUtil.write(ensureCommitMetaFile(path), String.valueOf(commitId));
    }

    private int readVersion(File path) throws IOException {
        String s = StringUtil.trimToNull(FileUtil.read(ensureVersionMetaFile(path)));
        return FormatUtil.parseIntValue(s, -1);
    }

    private void writeVersion(File path, int version) throws IOException {
        FileUtil.write(ensureVersionMetaFile(path), String.valueOf(version));
    }

    private void scheduleIndex() {
        long interval = 1000L * options.getIndexCheckSeconds();
        indexTimer.schedule(new SearchIndexTask(), interval);
    }

    private void scheduleFullIndex() {
        if (options.getFullIndexMinutes() > 0) {
            fullIndexTimer.schedule(new FullIndexTask(), DateUtil.MILLIS_PER_MINUTE * options.getFullIndexMinutes());
        }
    }

    private File getCurrentSymbol() {
        return new File(dir, "current");
    }

    private File getCurrentCanonicalPath() throws IOException {
        File current = getCurrentSymbol();
        if (!current.exists()) {
            return null;
        }
        File canonical = current.getCanonicalFile();
        if (!canonical.exists()) {
            return null;
        }
        return canonical;
    }

    private static boolean closeDirectory(Directory directory) {
        if (directory != null) {
            try {
                directory.close();
            } catch (Throwable e) {
                LOG.error(null, e);
                return false;
            }
        }
        return true;
    }

    private boolean containsIndex(File directory) {
        File indexDir = new File(directory, "data");
        // faster than indexDir.list
        if (new File(indexDir, "write.lock").exists()) {
            return true;
        }
        return indexDir.list().length > 0;
    }

    private boolean setCurrentPath(File newPath) {
        synchronized (PATH_WRITE_LOCK) {
            stagePath = null;
            if (newPath == null) {
                return false;
            }
            DirectoryReader newReader = null;
            if (containsIndex(newPath)) {
                newReader = openReader(newPath);
                if (newReader == null) {
                    LOG.error("Failed to open reader");
                    return false;
                }
            }
            // set new path
            dataPath = newPath;
            setReader(newReader);
            // create symbol (ignore errors)
            try {
                File currentSymbol = getCurrentSymbol();
                currentSymbol.delete();
                Files.createSymbolicLink(currentSymbol.toPath(), dataPath.toPath());
            } catch (Throwable t) {
                LOG.error("Failed to createSymbolicLink for current path", t);
            }
            try {
                indexTimer.schedule(new RemoveOldIndexTask(), 120 * 1000);
            } catch (Throwable t) {
                LOG.error(null, t);
            }
            return true;
        }
    }

    private boolean fullIndex() {
        if (VERBOSE) {
            verbose("Start full index......");
        }
        File newPath = doFullIndex();
        boolean ok = setCurrentPath(newPath);
        if (VERBOSE) {
            verbose("End full index: ok - " + ok);
        }
        return ok;
    }

    private File doFullIndex() {
        File newPath = null;
        Directory directory = null;
        long lastCommitId;
        synchronized (PATH_WRITE_LOCK) {
            try {
                long t = System.currentTimeMillis();
                while (true) {
                    DateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
                    format.setTimeZone(FormatUtil.GMT_TIMEZONE);
                    String name = format.format(new Date(t));
                    File file = new File(dir, name);
                    if (file.exists()) {
                        t++;
                        continue;
                    }
                    newPath = file;
                    break;
                }
                directory = openDirectory(newPath);
                // mark last commit id
                lastCommitId = options.getCommitRepository().getLastCommitId();
                if (lastCommitId < 0) {
                    lastCommitId = 0;
                }
                writeCommitId(newPath, lastCommitId);
                writeVersion(newPath, options.getVersion());
            } catch (Throwable e) {
                LOG.error(null, e);
                closeDirectory(directory);
                return null;
            }
            stagePath = newPath;
        }

        DocRepository docRepository = options.getDocRepository();
        Object lastId = null;
        int batch = options.getCommitLoadBatch();
        int counter = 0;
        while (true) {
            IndexWriter writer = null;
            try {
                DocRoster roster = docRepository.findDocs(lastId, batch);
                if (roster == null) {
                    break;
                }
                lastId = roster.lastId;
                if (roster.items != null && !roster.items.isEmpty()) {
                    writer = new IndexWriter(directory, new IndexWriterConfig(analyzer));
                    counter += roster.items.size();
                    for (Map<String, Object> doc : roster.items) {
                        try {
                            Document document = parseDocument(doc);
                            if (VERBOSE) {
                                verbose("AddDocument: " + doc + "\n" + document);
                            }
                            writer.addDocument(document);
                        } catch (Exception e) {
                            LOG.error(null, e);
                        }
                    }
                }
                if (lastId == null) {
                    break;
                }
            } catch (Throwable e) {
                LOG.error(null, e);
                sleepAWhile();
            } finally {
                FileUtil.close(writer);
            }
        }
        closeDirectory(directory);

        if (VERBOSE) {
            verbose("Full index documents: " + counter);
        }

        // amend
        int commits = indexFromCommits(newPath, lastCommitId);
        if (commits < 0) {
            return null;
        }

        return newPath;
    }

    private void sleepAWhile() {
        try {
            Thread.sleep(1000);
        } catch (Throwable e) {
            LOG.error(null, e);
        }
    }

    private int indexFromCommits(File path, long commitId) {
        Directory directory = null;
        try {
            if (commitId < 0) {
                commitId = readCommitId(path);
                if (commitId < 0) {
                    LOG.error("commit < 0 in " + path);
                    return -1;
                }
            }
            directory = openDirectory(path);
        } catch (Throwable e) {
            LOG.error(null, e);
            closeDirectory(directory);
            return -1;
        }
        if (VERBOSE) {
            verbose("Index from commit: " + commitId);
        }
        CommitRepository commitRepository = options.getCommitRepository();
        int batch = options.getCommitLoadBatch();
        boolean hasCommit = false;
        while (true) {
            IndexWriter writer = null;
            try {
                List<Commit> commits = commitRepository.findCommit(commitId, batch);
                if (commits.isEmpty()) {
                    break;
                }
                writer = new IndexWriter(directory, new IndexWriterConfig(analyzer));
                for (Commit commit : commits) {
                    switch (commit.getType()) {
                    case Commit.TYPE_SAVE: {
                        Map<?, ?> payloadAsMap = (Map<?, ?>) commit.getPayload();
                        if (payloadAsMap != null) {
                            Object id = payloadAsMap.get(options.getIdFieldName());
                            Document document = parseDocument(payloadAsMap);
                            if (VERBOSE) {
                                verbose("SaveDocument: " + payloadAsMap + "\n" + document);
                            }
                            writer.updateDocument(getIdTerm(id), document);
                            hasCommit = true;
                        }
                        break;
                    }
                    case Commit.TYPE_REMOVE: {
                        Object id = commit.getPayload();
                        if (VERBOSE) {
                            verbose("RemoveDocument: " + id);
                        }
                        writer.deleteDocuments(getIdTerm(id));
                        hasCommit = true;
                        break;
                    }
                    default:
                        LOG.error("Unknow commit type: " + commit.getType());
                    }
                }
                long newCommitId = commits.get(commits.size() - 1).getId();
                writeCommitId(path, newCommitId);
                commitId = newCommitId;
                if (commits.size() < batch) {
                    break;
                }
            } catch (Throwable e) {
                LOG.error(null, e);
                sleepAWhile();
            } finally {
                FileUtil.close(writer);
            }
        }
        closeDirectory(directory);
        return hasCommit ? 1 : 0;
    }

    private Directory openDirectory(File path) throws Exception {
        if (path == null) {
            throw new NullPointerException();
        }
        File dataDir = new File(path, "data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        return FSDirectory.open(dataDir.toPath(), FSLockFactory.getDefault());
    }

    private Object deserializeFieldValue(IndexableField field) {
        if (field == null) {
            return null;
        }
        SearchField fieldDef = options.getFieldMap().get(field.name());
        if (fieldDef == null || fieldDef.getType() == SearchFieldType.TYPE_STRING) {
            return field.stringValue();
        }
        return field.numericValue();
    }

    private Term getIdTerm(Object id) {
        SearchField idField = options.getIdField();
        switch (idField.getType()) {
        case SearchFieldType.TYPE_STRING: {
            return new Term(idField.getName(), FormatUtil.parseString(id));
        }
        case SearchFieldType.TYPE_INT: {
            BytesRefBuilder brb = new BytesRefBuilder();
            NumericUtils.intToPrefixCoded(FormatUtil.parseInteger(id).intValue(), 0, brb);
            return new Term(idField.getName(), brb.get());
        }
        case SearchFieldType.TYPE_LONG: {
            BytesRefBuilder brb = new BytesRefBuilder();
            NumericUtils.longToPrefixCoded(FormatUtil.parseLong(id).longValue(), 0, brb);
            return new Term(idField.getName(), brb.get());
        }
        default:
            throw new RuntimeException("No supported id type: " + idField.getType());
        }
    }

    private Document parseDocument(Map<?, ?> docAsMap) {
        Document doc = new Document();
        LinkedHashSet<String> tokens = null;
        if (options.getGeneralFieldName() != null) {
            tokens = new LinkedHashSet<String>();
        }

        for (Entry<?, ?> entry : docAsMap.entrySet()) {
            String key = entry.getKey().toString();
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            SearchField field = options.getFieldMap().get(key);
            Field f = null;
            String strValue = null;
            if (field == null) {
                if (value instanceof Map) {
                    field = new SearchField(SearchFieldType.TYPE_STRING);
                    Map<?, ?> valueAsMap = (Map<?, ?>) value;
                    strValue = FormatUtil.parseString(valueAsMap.get("value"));
                    field.setName(key);
                    field.setStore(FormatUtil.parseBoolean(valueAsMap.get("store"), false));
                    field.setIndex(SearchIndexOptions.parse(FormatUtil.parseString(valueAsMap.get("index"))));
                    field.setGeneralSearch(FormatUtil.parseBoolean(valueAsMap.get("generalSearch"), false));
                    field.setSegType(SearchField.parseSegType(FormatUtil.parseString(valueAsMap.get("segTypes")),
                            SegType.COMPLEX));
                }
            } else {
                switch (field.getType()) {
                case SearchFieldType.TYPE_STRING: {
                    strValue = value.toString();
                    break;
                }
                case SearchFieldType.TYPE_INT: {
                    f = new IntField(key, FormatUtil.parseInteger(value),
                            numberFieldType(field.isStore() ? IntField.TYPE_STORED : IntField.TYPE_NOT_STORED,
                                    field.getIndex() != null));
                    break;
                }
                case SearchFieldType.TYPE_LONG: {
                    f = new LongField(key, FormatUtil.parseLong(value),
                            numberFieldType(field.isStore() ? LongField.TYPE_STORED : LongField.TYPE_NOT_STORED,
                                    field.getIndex() != null));
                    break;
                }
                case SearchFieldType.TYPE_FLOAT: {
                    f = new FloatField(key, FormatUtil.parseFloat(value),
                            numberFieldType(field.isStore() ? FloatField.TYPE_STORED : FloatField.TYPE_NOT_STORED,
                                    field.getIndex() != null));
                    break;
                }
                case SearchFieldType.TYPE_DOUBLE: {
                    f = new DoubleField(key, FormatUtil.parseDouble(value),
                            numberFieldType(field.isStore() ? DoubleField.TYPE_STORED : DoubleField.TYPE_NOT_STORED,
                                    field.getIndex() != null));
                    break;
                }
                default:
                    throw new RuntimeException("Bad field type: " + field.getType());
                }
            }
            if (StringUtil.isNotEmpty(strValue)) {
                boolean store = field.isStore();
                SearchIndexOptions index = field.getIndex();
                if (index != null || store) {
                    FieldType fType = new FieldType();
                    fType.setStored(store);
                    if (index != null) {
                        fType.setIndexOptions(index.getIndex());
                        fType.setTokenized(index.isTokenized());
                    } else {
                        fType.setIndexOptions(IndexOptions.NONE);
                    }
                    if (field.getSegType() != SegType.COMPLEX) {
                        if (store) {
                            throw new RuntimeException("Could not set segType if stored of field: " + field.getName());
                        }
                        LinkedHashSet<String> fieldTokens = new LinkedHashSet<String>();
                        SegUtil.seg(strValue, fieldTokens, field.getSegType());
                        String searchString = StringUtil.join(fieldTokens, TOKEN);
                        f = new Field(key, searchString, fType);
                    } else {
                        f = new Field(key, strValue, fType);
                    }
                }
                if (field.isGeneralSearch() && tokens != null) {
                    SegUtil.seg(options.isGeneralFieldLowercase() ? strValue.toLowerCase() : strValue, tokens,
                            options.getGeneralFieldSegType());
                }
            }
            if (f == null) {
                continue;
            }
            doc.add(f);
        }

        if (tokens != null && !tokens.isEmpty()) {
            String searchString = StringUtil.join(tokens, TOKEN);
            FieldType fType = new FieldType();
            fType.setStored(false);
            fType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
            fType.setTokenized(true);
            doc.add(new Field(options.getGeneralFieldName(), searchString, fType));
        }

        if (doc.getField(options.getIdFieldName()) == null) {
            throw new IllegalArgumentException("No id set for doc");
        }
        return doc;
    }

    private static FieldType numberFieldType(FieldType raw, boolean index) {
        FieldType newType = new FieldType(raw);
        if (index) {
            newType.setDocValuesType(DocValuesType.NUMERIC);
            newType.setTokenized(false);
        } else {
            newType.setIndexOptions(IndexOptions.NONE);
        }
        newType.freeze();
        return newType;
    }

    public void start() throws Exception {
        synchronized (PATH_WRITE_LOCK) {
            if (started) {
                throw new RuntimeException("Already call start method");
            }
            started = true;
        }
        File current = getCurrentCanonicalPath();
        long commitId = -1;
        if (current != null) {
            // compare version
            int currentVersion = readVersion(current);
            if (currentVersion == options.getVersion()) {
                commitId = readCommitId(current);
            }
        }
        boolean ok = false;
        if (commitId < 0) {
            ok = fullIndex();
        } else {
            indexFromCommits(current, commitId);
            ok = setCurrentPath(current);
        }
        if (!ok) {
            throw new RuntimeException("Failed to build full index or index from commits");
        }
        // schedule index from commit
        scheduleIndex();
        // schedule full index (if necessary)
        if (options.getFullIndexMinutes() > 0) {
            fullIndexTimer = new Timer();
            scheduleFullIndex();
        }
        // schedule close old reader task
        {
            long closeReaderPeriod = 1000L * options.getIndexCheckSeconds() / 2;
            closeReaderPeriod = Math.min(closeReaderPeriod, 20 * 1000);
            if (VERBOSE) {
                verbose("closeReaderPeriod: " + closeReaderPeriod + "ms");
            }
            indexTimer.schedule(new CloseReaderTask(), 0, closeReaderPeriod);
        }
        ok = testSearch();
        if (!ok) {
            throw new RuntimeException("Test search failed");
        }
    }

    private boolean testSearch() {
        boolean ok = false;
        try {
            search(null, null, null, 0, 1, true);
            ok = true;
        } catch (Throwable t) {
            LOG.error(null, t);
        }
        if (VERBOSE) {
            verbose("test search " + (ok ? "ok" : "failed"));
        }
        return ok;
    }

    private DirectoryReader openReader(File path) {
        DirectoryReader reader = null;
        try {
            Directory directory = openDirectory(path);
            reader = DirectoryReader.open(directory);
            return reader;
        } catch (Throwable t) {
            LOG.error(null, t);
            FileUtil.close(reader);
            return null;
        }
    }

    private void setReader(DirectoryReader newReader) {
        // set new reader and close old reader
        DirectoryReader oldReader = directoryReader;
        directoryReader = newReader;
        if (oldReader != null) {
            synchronized (CLOSE_READER_LOCK) {
                closingReaders.add(new SearchReader(oldReader));
            }
        }
    }

    public Pagination<Map<String, Object>> search(Query query, SortField[] sortFields, Collection<String> fields,
            int offset, int limit, boolean countTotal) throws Exception {
        if (offset < 0) {
            offset = 0;
        }
        if (limit <= 0) {
            limit = 10;
        }
        if (query == null) {
            query = new MatchAllDocsQuery();
        }
        Sort sort = null;
        if (sortFields != null) {
            sort = new Sort(sortFields);
        } else {
            sort = new Sort(SortField.FIELD_SCORE);
        }
        DirectoryReader reader = directoryReader;
        if (reader == null) {
            return Pagination.emptyInstance(limit);
        }
        IndexSearcher searcher = new IndexSearcher(reader);
        int total = 0;
        if (countTotal) {
            total = searcher.count(query);
            if (total <= offset) {
                return Pagination.emptyInstance(limit);
            }
        }
        ScoreDoc[] hits = searcher.search(query, offset + limit, sort, false, false).scoreDocs;
        List<Map<String, Object>> items = new LinkedList<Map<String, Object>>();
        boolean specifyFields = fields != null && !fields.isEmpty();
        for (int i = offset; i < hits.length; i++) {
            Document doc = searcher.doc(hits[i].doc);
            Map<String, Object> item = null;
            if (specifyFields) {
                item = new HashMap<String, Object>(fields.size());
                for (String fieldName : fields) {
                    item.put(fieldName, deserializeFieldValue(doc.getField(fieldName)));
                }
            } else {
                List<IndexableField> iFields = doc.getFields();
                item = new HashMap<String, Object>(iFields.size());
                for (IndexableField iField : iFields) {
                    item.put(iField.name(), deserializeFieldValue(iField));
                }
            }
            items.add(item);
        }
        return new Pagination<Map<String, Object>>(items, total, offset, limit);
    }

    public SearcherOptions getOptions() {
        return options;
    }
}
