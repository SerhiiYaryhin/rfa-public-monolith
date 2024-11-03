package media.toloka.rfa.security.model;

public enum ERole {
//    ROLE_USER("User"),
//    ROLE_CREATOR("Creator"),
//    ROLE_MODERATOR("Moderator"),
//    ROLE_EDITOR("Editor"),
//    ROLE_ADMIN("Admin");
    ROLE_UNKNOWN("ROLE_UNKNOWN"),       // 0
    ROLE_USER("ROLE_USER"),             // 1
    ROLE_CREATER("ROLE_CREATER"),       // 2
    ROLE_MODERATOR("ROLE_MODERATOR"),   // 3
    ROLE_EDITOR("ROLE_EDITOR"),         // 4
    ROLE_ADMIN("ROLE_ADMIN"),           // 5
    ROLE_TELEGRAM("ROLE_TELEGRAM");

    public final String label;

    private ERole(String label) {
        this.label = label;
    }
}