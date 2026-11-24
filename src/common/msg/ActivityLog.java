// 2271424 이상언
package common.msg;

// [20] 최근 활동 한 줄 (S -> C)
public class ActivityLog extends Message {
    private static final long serialVersionUID = 1L;

    public String text;

    public ActivityLog(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "[활동] " + text;
    }
}
