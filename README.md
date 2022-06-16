# Ferent

A Clojure library  to calculate dependency metrics
measurements on the model of [JDepend](https://github.com/clarkware/jdepend).

## Output
For example
```clojure

{"project-a"    {:arrow-in 2, :arrow-out 10, :instability 0.83},
 ;...
 :project-count 44,
 :cycles        () ; cycles between projects; avoid these!

 }
```
### Explanation
* For each project, 
  * `:arrow-in` is the number of dependees (called "afferent" in JDepend).
  * `:arrow-out`  is the number of dependencies (called "efferent" in JDepend).
  * `:instability` is the count of dependees as a 
  fraction of all links for this project 
  (dependencies plus dependees). The idea is that a project 
  that depends on lots of others will be impact if any change 
  and so  should be  non-infrastructural, at the application-level.
* `:project-count`  is the number of projects in this organization that were analyzed.
* `cycles` shows cycles of dependency links among the projects.
* Links to unknown projects -- e.g., outside the organization -- 
are unknown. This is because your system architecture generally 
lives inside an organization and any links outside it are continued
outside integrations.

## Usage
### Permissions
Use Organization Administrator or any role that will allow you to
list project, to see what IAM roles each grants, and what service accounts
are in each one.

### Environment variables
* There are no command-line arguments.
* Instead, set  env variables:
  * `ORG_ID`
    * For example `9872345612389`
    * This is mandatory unless you pass a `PROJECT_FILE` (see below)
    * Ferent will retrieve a list of all projects in this organization
    for which you have the right permissions, 
    * and track dependencies between them. 
    
  * `QUERY_FILTER`
    * For example `NOT displayName=doitintl* AND NOT projectId=sys-*`
    * This is optional. Set it to make the querying more efficient.
    * See docs [here](https://cloud.google.com/workflows/docs/reference/googleapis/cloudresourcemanager/v3/projects/search)
    * The default is `NOT projectId=sys-*``
  * `QUERY_PAGE_SIZE`
    * For example `950`
    * This is optional and usually you will not set it.
    * The default is 1000.
  * `PROJECTS_FILE`
    * The relative or absolute path to a file with a list of projects in EDN format.
    * If used then the list of projects will not be queried out of GCP, 
   though the list of permissions and service accounts for each project will be. 
    

## License

See `LICENSE.md`