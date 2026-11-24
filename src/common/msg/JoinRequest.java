// 2271424 이상언
package common.msg;

// [2] 입장 요청 (C -> S), 연결 후 첫 메시지
public class JoinRequest extends Message {
    private static final long serialVersionUID = 1L;

    public String userName;
    public String sessionToken; // 첫 접속은 null

    public JoinRequest(String userName, String sessionToken) {
        this.userName = userName;
        this.sessionToken = sessionToken;
    }

    @Override
    public String toString() {
        return "[입장요청] " + userName;
    }
}
