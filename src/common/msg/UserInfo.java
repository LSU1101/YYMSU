// 2271424 이상언
package common.msg;

import java.io.Serializable;

// UserListUpdate 안에 들어가는 참가자 한 명의 정보
public class UserInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    public String name;
    public boolean host; // 방장 여부
    public String status; // "ONLINE", "AWAY", "BUSY", "DISCONNECTED"

    public UserInfo(String name, boolean host, String status) {
        this.name = name;
        this.host = host;
        this.status = status;
    }

    @Override
    public String toString() {
        return name + "/" + status;
    }
}
