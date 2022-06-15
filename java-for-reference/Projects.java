import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.cloudresourcemanager.v3.CloudResourceManager;
import com.google.api.services.iam.v1.IamScopeimport com.google.auth.http.HttpCredentialsAdapter;
        s;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;

public class Projects {
    static String organizationsPfx = "organizations/";
    public static void main(String[] args) throws Exception {

        var org =args.length>0?args[0]:"976583563296";
        var queryS=args.length>1?args[1]:"NOT displayName=doitintl* AND NOT projectId=sys-*";

        var desiredOrgPath= organizationsPfx +org;
        String tok = null;

        var svc = initializeService();
        var  projectsInOrg= new ArrayList<String>();
        var  projectsUnknownIfInOrg= new ArrayList<String>();

        do {
            tok = oneSearchQuery(queryS, desiredOrgPath, tok, svc, projectsInOrg, projectsUnknownIfInOrg);
        } while (tok != null);

        System.out.println(projectsInOrg.size());
        System.out.println(projectsUnknownIfInOrg.size());
    }

    private static String oneSearchQuery(String queryS,
                                         String desiredOrgPath,
                                         String tok,
                                         CloudResourceManager svc,
                                         ArrayList<String> projectsInOrg,
                                         ArrayList<String> projectsUnknownIfInOrg) throws IOException {
        var projectSearch = svc.projects().search();
        projectSearch.setQuery(queryS).setPageToken(tok).setPageSize(900);
        var searched = projectSearch.execute();
        final var projs = searched.getProjects();
        for (var project : projs) {
            var projectId = project.getProjectId();
            var parent = project.getParent();
            if (desiredOrgPath.equals(parent)){
                projectsInOrg.add(projectId);
            }else if (!parent.startsWith(organizationsPfx)   ){
                projectsUnknownIfInOrg.add(projectId);
            }//else in another org, not in our org
        }
        tok = searched.getNextPageToken();
        return tok;
    }

    public static CloudResourceManager initializeService()
            throws IOException, GeneralSecurityException {
        GoogleCredentials credential =
                GoogleCredentials.getApplicationDefault()
                        .createScoped(Collections.singleton(IamScopes.CLOUD_PLATFORM));

        CloudResourceManager service =
                new CloudResourceManager.Builder(
                        GoogleNetHttpTransport.newTrustedTransport(),
                        GsonFactory.getDefaultInstance(),
                        new HttpCredentialsAdapter(credential))
                        .setApplicationName("project-listing")
                        .build();
        return service;
    }
}
