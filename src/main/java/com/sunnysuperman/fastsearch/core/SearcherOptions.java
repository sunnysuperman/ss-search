package com.sunnysuperman.fastsearch.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.sunnysuperman.fastsearch.commit.CommitRepository;
import com.sunnysuperman.fastsearch.doc.DocRepository;

public class SearcherOptions {
    private String name;
    private String dir;
    private String idFieldName;
    private String generalFieldName;
    private Map<String, SearchField> fieldMap;
    private byte generalFieldSegType = SegType.COMPLEX_MAXWORD;
    private boolean generalFieldLowercase = true;
    private CommitRepository commitRepository;
    private DocRepository docRepository;
    private int indexCheckSeconds = 5;
    private int commitLoadBatch = 100;
    private int fullIndexMinutes = 0;
    private int version;

    private volatile SearchField idField;
    private volatile List<SearchField> fields;
    private volatile boolean frozen;

    public SearcherOptions froze() {
        if (frozen) {
            return this;
        }
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (dir == null) {
            throw new NullPointerException("dir");
        }
        if (idFieldName == null) {
            throw new NullPointerException("idFieldName");
        }
        if (fieldMap == null) {
            throw new NullPointerException("fieldMap");
        }
        if (commitRepository == null) {
            throw new NullPointerException("commitRepository");
        }
        if (docRepository == null) {
            throw new NullPointerException("docRepository");
        }
        if (indexCheckSeconds <= 0) {
            throw new IllegalArgumentException("indexCheckSeconds");
        }
        if (commitLoadBatch <= 0) {
            throw new IllegalArgumentException("commitLoadBatch");
        }
        idField = fieldMap.get(idFieldName);
        if (idField == null) {
            throw new NullPointerException("idField");
        }
        fields = new ArrayList<SearchField>(fieldMap.values());
        frozen = true;
        return this;
    }

    private void checkFrozen() {
        if (frozen) {
            throw new RuntimeException("Could not set after frozen");
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        checkFrozen();
        this.name = name;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        checkFrozen();
        this.dir = dir;
    }

    public String getIdFieldName() {
        return idFieldName;
    }

    public void setIdFieldName(String idFieldName) {
        checkFrozen();
        this.idFieldName = idFieldName;
    }

    public String getGeneralFieldName() {
        return generalFieldName;
    }

    public void setGeneralFieldName(String generalFieldName) {
        checkFrozen();
        this.generalFieldName = generalFieldName;
    }

    public boolean isGeneralFieldLowercase() {
        return generalFieldLowercase;
    }

    public void setGeneralFieldLowercase(boolean generalFieldLowercase) {
        checkFrozen();
        this.generalFieldLowercase = generalFieldLowercase;
    }

    public Map<String, SearchField> getFieldMap() {
        return fieldMap;
    }

    public void setFieldMap(Map<String, SearchField> fieldMap) {
        checkFrozen();
        this.fieldMap = Collections.unmodifiableMap(fieldMap);
    }

    public byte getGeneralFieldSegType() {
        return generalFieldSegType;
    }

    public void setGeneralFieldSegType(byte generalFieldSegType) {
        checkFrozen();
        this.generalFieldSegType = generalFieldSegType;
    }

    public CommitRepository getCommitRepository() {
        return commitRepository;
    }

    public void setCommitRepository(CommitRepository commitRepository) {
        checkFrozen();
        this.commitRepository = commitRepository;
    }

    public DocRepository getDocRepository() {
        return docRepository;
    }

    public void setDocRepository(DocRepository docRepository) {
        checkFrozen();
        this.docRepository = docRepository;
    }

    public int getIndexCheckSeconds() {
        return indexCheckSeconds;
    }

    public void setIndexCheckSeconds(int indexCheckSeconds) {
        checkFrozen();
        this.indexCheckSeconds = indexCheckSeconds;
    }

    public int getCommitLoadBatch() {
        return commitLoadBatch;
    }

    public void setCommitLoadBatch(int commitLoadBatch) {
        checkFrozen();
        this.commitLoadBatch = commitLoadBatch;
    }

    public int getFullIndexMinutes() {
        return fullIndexMinutes;
    }

    public void setFullIndexMinutes(int fullIndexMinutes) {
        this.fullIndexMinutes = fullIndexMinutes;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public SearchField getIdField() {
        return idField;
    }

    public List<SearchField> getFields() {
        return fields;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

}
