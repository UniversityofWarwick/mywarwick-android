package uk.ac.warwick.my.app.user;

public class AuthenticatedUser implements User {

    private final String usercode;
    private final String name;
    private final String photoUrl;

    @Override
    public boolean isSignedIn() {
        return usercode != null;
    }

    public AuthenticatedUser(final String usercode, final String name, final String photoUrl) {
        this.usercode = usercode;
        this.name = name;
        this.photoUrl = photoUrl;
    }

    @Override
    public String getUsercode() {
        return usercode;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPhotoUrl() {
        return photoUrl;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AuthenticatedUser) {
            AuthenticatedUser other = (AuthenticatedUser) obj;

            return getUsercode().equals(other.getUsercode());
        }

        return false;
    }
}
