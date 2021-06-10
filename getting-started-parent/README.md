Running `mvn quarkus:dev` inside *getting-started-parent/getting-started-module1/module1* directory works fine.

After adding new dependency in parent module of *module1* i.e. in *getting-started-module1/pom.xml* (e.x. quarkus-smallrye-openapi deps), changes in dev mode
won't be visible for *module1*.
