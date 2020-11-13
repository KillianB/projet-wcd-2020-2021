
package foo;


import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.api.server.spi.response.UnauthorizedException;

import java.util.ArrayList;
import java.util.List;

import com.google.api.server.spi.auth.EspAuthenticator;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.PropertyProjection;
import com.google.appengine.api.datastore.PreparedQuery.TooManyResultsException;
import com.google.appengine.api.datastore.Query.CompositeFilter;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.QueryResultList;
import com.google.appengine.api.datastore.Transaction;

@Api(name = "tinyApi",
     version = "v1",
     audiences = "1048463456874-56t3t922794hiac34phbfntkdqmt0lhl.apps.googleusercontent.com",
  	 clientIds = "1048463456874-56t3t922794hiac34phbfntkdqmt0lhl.apps.googleusercontent.com",
     namespace =
     @ApiNamespace(
		   ownerDomain = "https://tinyinsta-295118.ew.r.appspot.com",
		   ownerName = "https://tinyinsta-295118.ew.r.appspot.com",
		   packagePath = "")
     )

public class Endpoint {

	@ApiMethod(name="getTimeline", httpMethod = HttpMethod.GET)
	public List<Post> getTimeLine() {
		List<Post> result = new ArrayList<Post>();

		return result;
	}
}