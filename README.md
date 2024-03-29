# Ferent

A Clojure library to calculate dependency metrics   on the model
of [JDepend](https://github.com/clarkware/jdepend).

See [this associated article](https://www.usenix.org/publications/loginonline/untangling-cloud) for background.

For each project, we calculate coupling based on the count of service accounts in other projects that are granted a
role in this project. The idea is that the other project "knows about"
this project and is impacted if this project changes.

Though there can be dependencies not reflected in service accounts, it is reasonable to use service accounts to track
dependencies if we assume that

* internal integrations are authenticated
  (i.e., internal websites are not like public websites that can be left open for unauthenticated use);
* inter-service authentication is done with service accounts (which is the best practice).

## Output

### Example

```clojure

{"project-a"    {:arrow-in 2, :arrow-out 10, :instability 0.83},
 ;...
 :project-count 44,
 :cycles        (["project-a" "project-b" "project-c"]
                 ["project-b" "project-c" ]) 

 }
```

### Explanation

* For each project
    * `:arrow-out` is the number of dependencies (called "efferent" in JDepend).
    * `:arrow-in` is the number of dependees (called "afferent" in JDepend).
    * `:instability` is the count of dependees as a fraction of all links for this project
      (dependencies plus dependees). The idea is that a project that depends on lots of others is at risk of being
      impacted by a change and so should be non-infrastructural -- it should be at the application-level.
* `:project-count` is the number of projects in this organization that were analyzed.
* `:cycles` shows cycles of dependency links among the projects.
    * In the example above
        * `project-a` depends on `project-b`
          which depends on `project-c` which depends again on `project-a`, resulting in a cycle between the three
        * and in addition, `project-c` depends back on `project-b`, resulting in a cycle between the two.
    * Avoid cycles! A cycle effectively links everything in one unit, so if one change, all are potentially impacted.
* Links to unknown projects -- e.g., outside the organization -- are ignored. This is because your system architecture
  generally lives inside an organization, and any links outside it are considered to be outside integrations.

## The name

Ferent is short for "afferent" and "efferent". I use other terminology here, but in brief:

* Efferent links (Laten _ex_ and _ferens_ "leading out"), called `:arrow-out`, show the dependencies, the projects that
  this project depends on. A change in those projects might break this one.
* Afferent links (Laten _ad_ and _ferens_ "leading in"), called `:arrow-in` in the code, show the dependees, the
  projects that depend on a given project. A change in this project might break those.

## Usage

### Permissions

Use Organization Administrator or any role that will allow you to list projects,
to see what IAM roles each projects grants, and what service accounts are in each one.

### Prerequisites

* [Install Clojure](https://clojure.org/guides/install_clojure)
* [Install Leiningen](https://leiningen.org/)
* On Mac, `brew install   leiningen` should do it
* [Install gcloud cli](https://cloud.google.com/sdk/docs/install) and initialize.

### Running it
* See `example-usage.sh`
* The minimal usage is, `lein run --org-id 9999999999999`, where you substitute the organization ID.
* See more options below.

### Command-line arguments.

* `--org-id`
    * For example `999999999999`
    * This is mandatory.
    * Ferent will retrieve a list of all projects in this organization to which you have access (filtering using
      the `--query-filter` value), and track dependencies between them.
* `--query-filter`
    * For example

      `NOT displayName=generated-by-app-* AND NOT projectId=sys-*`

    * This is optional.
    * Set it to make the querying faster. If you have a lot of 
      projects autogenerated by some application (comparable to `sys-` which is autogenerated for Google Workspace
      AppScript and appears in the default filter), a correct filter can radically speed up the process.
      (in one test: from 8046 to 151 seconds)
    * See some (rather misformatted) docs on the query
      syntax [here](https://cloud.google.com/workflows/docs/reference/googleapis/cloudresourcemanager/v3/projects/search).
    * The default value is `NOT projectId=sys-*`, which excludes the many projects often generated by Google Workspace
      AppScript.

* `--projects-file`
    * The relative or absolute path to a file with a list of projects in EDN format.
    * This is optional, and generally you will not use it
      (since you probably don't have a list of projects, and Ferent can retrieve that for you).
    * If used, then the list of projects will not be queried out of GCP, though the list of permissions and service
      accounts for each project will be.

# Implementation note

Google's APIs for listing all projects in an organization are slow and require you to call the API too many times. The
code here does the listing as quickly as possible. 

Key steps

* Query all accessible projects using [CloudResourceManager][1]
  `projects()`
    * The query uses paging.
    * Use `setQuery` to accelerate the query with a filter by filtering out, for example, the hundreds of `sys-`
      projects often generated by AppScript.

* From the results:
    * We want to accept only relevant projects: Those that are in this org.
    * To do this, we accept those projects that are the child of the desired org and 
     reject those that are the direct child of a different org.
    * However, some projects are the child of a folder, so we need to walk 
     up the folder heirarchy. We do this (concurrently, for speed): 
      * Use
             `gcloud projects get-ancestors $PROJECT_ID`
             to find the organization.
        (I don't see a way to do that in Java, and so I call the CLI.)
      * Then, we accept those in the org; filter out those not in the org.

[1]: https://cloud.google.com/java/docs/reference/google-cloud-resourcemanager/latest/com.google.cloud.resourcemanager.v3

## Acknowledgement
I would like to thank Yehonatan Sharvit for all he has taught me about Clojure. 
The remaining errors are mine; hopefully my code will 
get even more Clojuery over time.

## License

See `LICENSE.md`