// 2271424 이상언
package common.msg;

import java.util.Vector;

// [7] 접속자 목록 (S -> C), 입장·퇴장할 때마다 서버가 전체에게 보냄
public class UserListUpdate extends Message {
    private static final long serialVersionUID = 1L;

    public Vector<UserInfo> users;

    public UserListUpdate(Vector<UserInfo> users) {
        this.users = users;
    }

    @Override
    public String toString() {
        return "[접속자] " + users;
    }
}
