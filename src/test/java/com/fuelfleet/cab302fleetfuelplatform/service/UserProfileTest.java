package com.fuelfleet.cab302fleetfuelplatform.service;

import com.fuelfleet.cab302fleetfuelplatform.PasswordHasher;
import com.fuelfleet.cab302fleetfuelplatform.dao.UserDao;
import com.fuelfleet.cab302fleetfuelplatform.db.ConnectionProvider;
import com.fuelfleet.cab302fleetfuelplatform.db.DBManager;
import com.fuelfleet.cab302fleetfuelplatform.exception.AuthorizationException;
import com.fuelfleet.cab302fleetfuelplatform.exception.ValidationException;
import com.fuelfleet.cab302fleetfuelplatform.model.Role;
import com.fuelfleet.cab302fleetfuelplatform.model.User;
import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class UserProfileTest {

    @TempDir
    Path directory;

    UserDao userDao;
    UserService userService;
    AppSession session;

    @BeforeEach
    void setup() throws Exception{
        ConnectionProvider connections = DBManager.provider(
                "jdbc:sqlite:" + directory.resolve("profile-test.db")

        );
        DBManager.initialize(connections);

        userDao = new UserDao(connections);
        session = new AppSession();

        User testUser = userDao.insert(
                "testmanager",
                "test-password-hash",
                Role.MANAGER
                );

        session.signIn(testUser);

        userService = new UserService(
                userDao,
                new PasswordHasher(),
                session
                );
    }

    @Test
    void validProfileUpdatesSuccessfully(){
        userService.updateMyProfile(
                "Test Manager",
                "manager@example.com"
                );
        UserDao.Profile profile = userService.getMyProfile();

        assertEquals("Test Manager", profile.fullName());
        assertEquals("manager@example.com", profile.contactEmail());
    }

    @Test
    void profileChangesPersistInDatabase() {
        userService.updateMyProfile(
                "Updated Manager",
                "updated@example.com"
        );

        UserDao anotherDao = new UserDao(
                DBManager.provider(
                        "jdbc:sqlite:" + directory.resolve("profile-test.db")
                )
        );

        UserDao.Profile saved = anotherDao
                .findProfileById(session.currentUser().orElseThrow().id())
                .orElseThrow();

        assertEquals("Updated Manager", saved.fullName());
        assertEquals("updated@example.com", saved.contactEmail());

    }

    @Test
    void invalidEmailIsRejected() {
        assertThrows(
                ValidationException.class,
                () -> userService.updateMyProfile(
                        "Test Manager",
                        "invalid-email"
                )
        );
    }
        @Test
        void blankNameIsRejected() {
            assertThrows(
                    ValidationException.class,
                    () -> userService.updateMyProfile(
                            "   ",
                            "manager@example.com"
                    )
            );
        }

        @Test
        void unauthenticatedUserCannotUpdateProfile(){
            AppSession loggedOutSession = new AppSession();

            UserService loggedOutService = new UserService(
                    userDao,
                    new PasswordHasher(),
                    loggedOutSession
                    );

            assertThrows(
                    AuthorizationException.class,
                    () -> loggedOutService.updateMyProfile(
                            "Test Manager",
                "manager@example.com"

            )

            );

    }


}