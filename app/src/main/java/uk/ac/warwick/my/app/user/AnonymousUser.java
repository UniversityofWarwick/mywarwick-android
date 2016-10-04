package uk.ac.warwick.my.app.user;

public class AnonymousUser implements User {

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
    public boolean equals(Object obj) {
        return obj instanceof AnonymousUser;
    }
}
