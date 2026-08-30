package cz.companyintel.web;

import static org.assertj.core.api.Assertions.assertThat;

import cz.companyintel.security.AuthorizationRules;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class MethodAuthorizationTest {

    @Test
    void protectsAllBusinessControllersWithTheReadPolicy() {
        assertClassPolicy(CompanyController.class, AuthorizationRules.READ);
        assertClassPolicy(PersonController.class, AuthorizationRules.READ);
        assertClassPolicy(TaskController.class, AuthorizationRules.READ);
        assertClassPolicy(ImportController.class, AuthorizationRules.READ);
        assertClassPolicy(AuditController.class, AuthorizationRules.READ);
        assertClassPolicy(DashboardController.class, AuthorizationRules.READ);
        assertClassPolicy(HealthController.class, AuthorizationRules.READ);
    }

    @Test
    void requiresEditorRoleForBusinessMutations() throws Exception {
        assertMethodPolicy(CompanyController.class, "save", AuthorizationRules.EDIT, CompanyRequest.class);
        assertMethodPolicy(CompanyController.class, "update", AuthorizationRules.EDIT,
                Long.class, CompanyUpdateRequest.class);
        assertMethodPolicy(CompanyController.class, "setWatchlisted", AuthorizationRules.EDIT,
                Long.class, WatchlistRequest.class);
        assertMethodPolicy(CompanyController.class, "assignPerson", AuthorizationRules.EDIT,
                Long.class, PersonAssignmentRequest.class);
        assertMethodPolicy(CompanyController.class, "updatePersonRole", AuthorizationRules.EDIT,
                Long.class, Long.class, PersonRoleUpdateRequest.class);
        assertMethodPolicy(CompanyController.class, "removePerson", AuthorizationRules.EDIT,
                Long.class, Long.class);
        assertMethodPolicy(PersonController.class, "update", AuthorizationRules.EDIT,
                Long.class, PersonUpdateRequest.class, org.springframework.security.core.Authentication.class);
        assertMethodPolicy(TaskController.class, "create", AuthorizationRules.EDIT, TaskRequest.class);
        assertMethodPolicy(TaskController.class, "update", AuthorizationRules.EDIT,
                Long.class, TaskRequest.class);
        assertMethodPolicy(TaskController.class, "setDone", AuthorizationRules.EDIT,
                Long.class, TaskRequest.class);
        assertMethodPolicy(TaskController.class, "setArchived", AuthorizationRules.EDIT,
                Long.class, TaskRequest.class);
        assertMethodPolicy(ImportController.class, "importJson", AuthorizationRules.EDIT, String.class);
        assertMethodPolicy(ImportController.class, "importCsv", AuthorizationRules.EDIT, String.class);
        assertMethodPolicy(ImportController.class, "previewJson", AuthorizationRules.EDIT, String.class);
        assertMethodPolicy(ImportController.class, "previewCsv", AuthorizationRules.EDIT, String.class);
        assertMethodPolicy(ImportController.class, "importAres", AuthorizationRules.EDIT, String.class);
    }

    @Test
    void reservesDestructiveAndAuditArchiveOperationsForAdministrators() throws Exception {
        assertMethodPolicy(CompanyController.class, "delete", AuthorizationRules.ADMIN, Long.class);
        assertMethodPolicy(PersonController.class, "delete", AuthorizationRules.ADMIN, Long.class);
        assertMethodPolicy(AuditController.class, "setArchived", AuthorizationRules.ADMIN,
                Long.class, AuditArchiveRequest.class);
        assertMethodPolicy(AuditController.class, "setArchived", AuthorizationRules.ADMIN,
                AuditBulkArchiveRequest.class);
    }

    private void assertClassPolicy(Class<?> controller, String expected) {
        PreAuthorize policy = controller.getAnnotation(PreAuthorize.class);
        assertThat(policy)
                .as("Autorizační pravidlo controlleru %s", controller.getSimpleName())
                .isNotNull();
        assertThat(policy.value()).isEqualTo(expected);
    }

    private void assertMethodPolicy(
            Class<?> controller,
            String methodName,
            String expected,
            Class<?>... parameterTypes) throws Exception {
        Method method = controller.getMethod(methodName, parameterTypes);
        PreAuthorize policy = method.getAnnotation(PreAuthorize.class);
        assertThat(policy)
                .as("Autorizační pravidlo %s.%s", controller.getSimpleName(), methodName)
                .isNotNull();
        assertThat(policy.value()).isEqualTo(expected);
    }
}
