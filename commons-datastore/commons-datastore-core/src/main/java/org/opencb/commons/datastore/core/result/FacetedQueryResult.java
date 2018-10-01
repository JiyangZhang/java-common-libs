/*
 * Copyright 2015-2017 OpenCB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opencb.commons.datastore.core.result;

import java.util.Collections;
import java.util.List;

/**
 * Created by jtarraga on 09/03/17.
 */
public class FacetedQueryResult extends AbstractResult {

    private String query;
    private FacetedQueryResultItem result;

    public FacetedQueryResult() {
    }

    @Deprecated
    public FacetedQueryResult(String id, int dbTime, long numTotalResults, String warningMsg, String errorMsg,
                              FacetedQueryResultItem result, String query) {
        this(id, dbTime, numTotalResults, Collections.singletonList(new Error(-1, "", warningMsg)), new Error(-1, "", errorMsg),
                result, query);
    }

    public FacetedQueryResult(String id, int dbTime, long numMatches, List<Error> warning, Error error,
                              FacetedQueryResultItem result, String query) {
        super(id, dbTime, numMatches, warning, error);
        this.result = result;
        this.query = query;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("FacetedQueryResult{");
        sb.append("id='").append(id).append('\'');
        sb.append(", dbTime=").append(dbTime);
        sb.append(", numMatches=").append(numMatches);
        sb.append(", warning='").append(warning).append('\'');
        sb.append(", error='").append(error).append('\'');
        sb.append(", query=").append(query);
        sb.append(", result=").append(result);
        sb.append('}');
        return sb.toString();
    }

    public String getQuery() {
        return query;
    }

    public FacetedQueryResult setQuery(String query) {
        this.query = query;
        return this;
    }

    public FacetedQueryResultItem getResult() {
        return result;
    }

    public FacetedQueryResult setResult(FacetedQueryResultItem result) {
        this.result = result;
        return this;
    }
}
