package io.quarkus.smallrye.graphql.deployment.federation.complex;

import io.smallrye.graphql.api.federation.Extends;
import io.smallrye.graphql.api.federation.External;
import io.smallrye.graphql.api.federation.Key;

@Key(fields = "id")
@Key(fields = "otherId")
@Extends
public class Bar {

    @External
    public int id;
    @External
    public String otherId;

    public String name;

}
