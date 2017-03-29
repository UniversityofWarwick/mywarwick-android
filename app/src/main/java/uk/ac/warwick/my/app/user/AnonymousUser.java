package uk.ac.warwick.my.app.user;

public class AnonymousUser implements User {

    private final boolean authoritative;

    public AnonymousUser(boolean authoritative) {
        this.authoritative = authoritative;
    }

    @Override
    public boolean isSignedIn() {
        return false;
    }

    @Override
    public String getUsercode() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getPhotoUrl() {
        return null;
    }

    @Override
    public boolean isAuthoritative() {
        return authoritative;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AnonymousUser) {
            AnonymousUser other = (AnonymousUser) obj;

            return isAuthoritative() == other.isAuthoritative();
        }

        return false;
    }
}
