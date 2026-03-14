package org.example.atuo_attend_backend.webhook;

import java.util.List;

public class GithubPushPayload {

    private String ref;
    private String before;
    private String after;
    private GithubRepository repository;
    private GithubPusher pusher;
    private List<GithubPushCommit> commits;

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getBefore() {
        return before;
    }

    public void setBefore(String before) {
        this.before = before;
    }

    public String getAfter() {
        return after;
    }

    public void setAfter(String after) {
        this.after = after;
    }

    public GithubRepository getRepository() {
        return repository;
    }

    public void setRepository(GithubRepository repository) {
        this.repository = repository;
    }

    public GithubPusher getPusher() {
        return pusher;
    }

    public void setPusher(GithubPusher pusher) {
        this.pusher = pusher;
    }

    public List<GithubPushCommit> getCommits() {
        return commits;
    }

    public void setCommits(List<GithubPushCommit> commits) {
        this.commits = commits;
    }

    public static class GithubRepository {
        private String name;
        private String full_name;
        private String clone_url;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getFull_name() {
            return full_name;
        }

        public void setFull_name(String full_name) {
            this.full_name = full_name;
        }

        public String getClone_url() {
            return clone_url;
        }

        public void setClone_url(String clone_url) {
            this.clone_url = clone_url;
        }
    }

    public static class GithubPusher {
        private String name;
        private String email;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    public static class GithubPushCommit {
        private String id;
        private String message;
        private String timestamp;
        private String url;
        private Author author;
        /** 本 commit 新增/修改/删除的文件路径列表（仅路径，无内容；国内无 diff 时可据此做 AI 降级分析） */
        private java.util.List<String> added;
        private java.util.List<String> modified;
        private java.util.List<String> removed;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public Author getAuthor() {
            return author;
        }

        public void setAuthor(Author author) {
            this.author = author;
        }

        public java.util.List<String> getAdded() {
            return added;
        }

        public void setAdded(java.util.List<String> added) {
            this.added = added;
        }

        public java.util.List<String> getModified() {
            return modified;
        }

        public void setModified(java.util.List<String> modified) {
            this.modified = modified;
        }

        public java.util.List<String> getRemoved() {
            return removed;
        }

        public void setRemoved(java.util.List<String> removed) {
            this.removed = removed;
        }

        public static class Author {
            private String name;
            private String email;

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getEmail() {
                return email;
            }

            public void setEmail(String email) {
                this.email = email;
            }
        }
    }
}

