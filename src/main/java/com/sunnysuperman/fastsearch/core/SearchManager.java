package com.sunnysuperman.fastsearch.core;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sunnysuperman.commons.bean.Bean;
import com.sunnysuperman.commons.config.PropertiesConfig;
import com.sunnysuperman.commons.util.FileUtil;
import com.sunnysuperman.commons.util.FileUtil.FileListHandler;
import com.sunnysuperman.commons.util.FormatUtil;
import com.sunnysuperman.commons.util.JSONUtil;
import com.sunnysuperman.fastsearch.commit.CommitRepository;
import com.sunnysuperman.fastsearch.doc.DocRepository;

public class SearchManager {
    private static final Logger LOG = LoggerFactory.getLogger(Searcher.class);
    private static final Map<String, Searcher> SEARCHERS = new HashMap<String, Searcher>(0);

    public static void start(Class<?> clazz, String dirPath) throws Exception {
        FileUtil.listClassPathFiles(clazz, dirPath, new FileListHandler() {

            @Override
            public void streamOpened(String fileName, String fullPath, InputStream in) throws Exception {
                if (fileName.startsWith("_")) {
                    return;
                }
                PropertiesConfig config = new PropertiesConfig(in);
                Map<String, Object> map = new HashMap<String, Object>(config.size());
                for (String key : config.keySet()) {
                    map.put(key, config.getValue(key));
                }
                String generalFieldSegType = FormatUtil.parseString(map.remove("generalFieldSegType"));
                String fieldsAsStr = map.remove("fields").toString();
                String commitRepository = map.remove("commitRepository").toString();
                String docRepository = map.remove("docRepository").toString();
                SearcherOptions options = Bean.fromMap(map, new SearcherOptions());
                options.setGeneralFieldSegType(SearchField.parseSegType(generalFieldSegType, SegType.COMPLEX_MAXWORD));
                options.setCommitRepository((CommitRepository) Class.forName(commitRepository).newInstance());
                options.setDocRepository((DocRepository) Class.forName(docRepository).newInstance());
                {
                    List<?> fields = JSONUtil.parseJSONArray(fieldsAsStr);
                    Map<String, SearchField> fieldMap = new HashMap<String, SearchField>(fields.size());
                    for (Object field : fields) {
                        Map<?, ?> fieldAsMap = (Map<?, ?>) field;
                        String name = fieldAsMap.get("name").toString();
                        int type = getFieldType(fieldAsMap.get("type").toString());
                        boolean store = FormatUtil.parseBoolean(fieldAsMap.get("store"), false);
                        String index = FormatUtil.parseString(fieldAsMap.get("index"));
                        boolean generalSearch = type == SearchFieldType.TYPE_STRING
                                ? FormatUtil.parseBoolean(fieldAsMap.get("generalSearch"), false)
                                : false;
                        SearchField f = new SearchField();
                        f.setName(name);
                        f.setType(type);
                        f.setStore(store);
                        f.setIndex(SearchIndexOptions.parse(index));
                        f.setGeneralSearch(generalSearch);
                        f.setSegType(SearchField.parseSegType(FormatUtil.parseString(fieldAsMap.get("segType")),
                                SegType.COMPLEX));
                        if (f.getIndex() == null && !f.isStore() && !f.isGeneralSearch()) {
                            throw new RuntimeException("Neither store nor index nor general-search on field: " + name);
                        }
                        fieldMap.put(f.getName(), f);
                    }
                    options.setFieldMap(fieldMap);
                }
                options = options.froze();
                if (LOG.isInfoEnabled()) {
                    LOG.info("Search options: " + JSONUtil.toJSONString(options));
                }
                if (SEARCHERS.containsKey(options.getName())) {
                    throw new RuntimeException("Duplicate name: " + options.getName());
                }
                SEARCHERS.put(options.getName(), new Searcher(options));
            }

            @Override
            public boolean willOpenStream(String fileName, String fullPath, boolean isDirectory) throws Exception {
                return true;
            }

        });
        for (Searcher searcher : SEARCHERS.values()) {
            searcher.start();
        }
    }

    private static int getFieldType(String s) throws Exception {
        s = s.toLowerCase();
        if (s.equals("string")) {
            return SearchFieldType.TYPE_STRING;
        } else if (s.equals("int")) {
            return SearchFieldType.TYPE_INT;
        } else if (s.equals("long")) {
            return SearchFieldType.TYPE_LONG;
        } else if (s.equals("float")) {
            return SearchFieldType.TYPE_FLOAT;
        } else if (s.equals("double")) {
            return SearchFieldType.TYPE_DOUBLE;
        } else {
            throw new IllegalArgumentException("Illegal field type: " + s);
        }
    }

    public static Searcher findSearcher(String name) {
        if (name == null) {
            return null;
        }
        return SEARCHERS.get(name);
    }
}
