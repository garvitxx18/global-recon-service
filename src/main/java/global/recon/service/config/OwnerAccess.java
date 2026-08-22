package global.recon.service.config;

import global.recon.service.service.ResourceNotFoundException;

public final class OwnerAccess {

    private OwnerAccess() {
    }

    public static void assertOwns(String ownerEmail) {
        String current = UserContext.require();
        if (ownerEmail == null || !current.equals(ownerEmail)) {
            throw new ResourceNotFoundException("Resource not found");
        }
    }
}
