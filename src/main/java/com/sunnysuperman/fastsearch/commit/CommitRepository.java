package com.sunnysuperman.fastsearch.commit;

import java.util.List;

public interface CommitRepository {

    List<Commit> findCommit(long afterCommitId, int limit);

    long getLastCommitId();

}
