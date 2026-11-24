// 2271424 이상언
package common.msg;

// [12] 그룹(칸) 만들기 (C -> S, S -> C)
public class Group extends Message {
    private static final long serialVersionUID = 1L;

    public String groupId; // 서버가 발급
    public String name;
    public int order;
    public String createdBy;

    public Group(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "[그룹] " + groupId + " " + name + " (만든 사람: " + createdBy + ")";
    }
}
