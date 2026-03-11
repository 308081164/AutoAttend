package org.example.atuo_attend_backend.commit.mapper;

import org.apache.ibatis.annotations.*;

@Mapper
public interface CommitDiffMapper {

    @Insert("""
            INSERT INTO aa_commit_diff(repo_full_name, commit_sha, diff_text, diff_size_bytes)
            VALUES(#{repoFullName}, #{commitSha}, #{diffText}, #{diffSizeBytes})
            """)
    int insert(@Param("repoFullName") String repoFullName,
               @Param("commitSha") String commitSha,
               @Param("diffText") String diffText,
               @Param("diffSizeBytes") long diffSizeBytes);

    @Select("""
            SELECT diff_text
            FROM aa_commit_diff
            WHERE repo_full_name = #{repoFullName} AND commit_sha = #{commitSha}
            """)
    String findDiffText(@Param("repoFullName") String repoFullName, @Param("commitSha") String commitSha);
}

