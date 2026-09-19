package au.edu.rmit.sept.webapp.user;

import au.edu.rmit.sept.webapp.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void theRegistrationPageIsPublicAndCarriesAnEmptyForm() throws Exception {

        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("registrationRequest"));
    }

    @Test
    void theLoginPageIsPublic() throws Exception {

        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void successfulRegistrationRedirectsToTheLoginPage() throws Exception {

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("name", "Ada Lovelace")
                        .param("email", "ada@example.com")
                        .param("password", "hunter2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        verify(userService).register(any(RegistrationRequest.class));
    }

    @Test
    void aDuplicateEmailRedisplaysTheFormWithAnError() throws Exception {

        doThrow(new IllegalArgumentException("An account with this email already exists"))
                .when(userService).register(any(RegistrationRequest.class));

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("name", "Ada Lovelace")
                        .param("email", "ada@example.com")
                        .param("password", "hunter2"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void registrationIsRejectedWithoutACsrfToken() throws Exception {

        mockMvc.perform(post("/register")
                        .param("name", "Ada Lovelace")
                        .param("email", "ada@example.com")
                        .param("password", "hunter2"))
                .andExpect(status().isForbidden());

        verify(userService, never()).register(any());
    }

    @Test
    void anonymousVisitorsAreSentToLoginWhenTheyAskForAProfile() throws Exception {

        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "ada@example.com")
    void aSignedInUserSeesTheirOwnProfile() throws Exception {

        User user = new User();
        user.setEmail("ada@example.com");
        when(userService.findByEmail("ada@example.com")).thenReturn(user);

        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attribute("user", user));
    }

    @Test
    @WithMockUser(username = "ada@example.com")
    void deactivatingAnAccountLogsTheUserOutAndReturnsHome() throws Exception {

        mockMvc.perform(post("/account/deactivate").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        verify(userService).deactivate("ada@example.com");
    }

    @Test
    void anonymousVisitorsCannotDeactivateAnAccount() throws Exception {

        mockMvc.perform(post("/account/deactivate").with(csrf()))
                .andExpect(status().is3xxRedirection());

        verify(userService, never()).deactivate(any());
    }
}
