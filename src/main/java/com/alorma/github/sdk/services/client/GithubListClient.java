package com.alorma.github.sdk.services.client;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;
import com.alorma.github.sdk.security.GitHub;
import com.alorma.github.sdk.security.InterceptingListOkClient;
import com.alorma.github.sdk.security.InterceptingOkClient;
import com.alorma.gitskarios.core.ApiClient;
import com.alorma.gitskarios.core.client.BaseClient;
import com.alorma.gitskarios.core.client.BaseListClient;
import com.alorma.gitskarios.core.client.PaginationLink;
import com.alorma.gitskarios.core.client.RelType;
import com.alorma.gitskarios.core.client.StoreCredentials;
import com.squareup.okhttp.OkHttpClient;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Header;
import retrofit.client.Response;

public abstract class GithubListClient<K>  extends BaseListClient<K> {

	public GithubListClient(Context context) {
		super(context, getApiClient(context));
	}

	private static ApiClient getApiClient(Context context) {
		String url = new StoreCredentials(context).getUrl();
		return new GitHub(url);
	}

	@Override
	public void intercept(RequestFacade request) {
		request.addHeader("Accept", getAcceptHeader());
		request.addHeader("User-Agent", "Gitskarios");
		request.addHeader("Authorization", "token " + getToken());
	}

	@Override
	public void log(String message) {
		Log.v("RETROFIT_LOG", message);
	}

	public String getAcceptHeader() {
		return "application/vnd.github.v3.json";
	}

	private int getLinkData(Response r) {
		List<Header> headers = r.getHeaders();
		Map<String, String> headersMap = new HashMap<String, String>(headers.size());
		for (Header header : headers) {
			headersMap.put(header.getName(), header.getValue());
		}

		String link = headersMap.get("Link");

		if (link != null) {
			String[] parts = link.split(",");
			try {
				PaginationLink bottomPaginationLink = new PaginationLink(parts[0]);
				if (bottomPaginationLink.rel == RelType.next) {
					return bottomPaginationLink.page;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return -1;
	}

	public abstract class BaseInfiniteCallback<T> implements Callback<T> {

		public BaseInfiniteCallback() {

		}

		@Override
		public void success(T t, Response response) {
			int nextPage = getLinkData(response);
			response(t);
			if (nextPage != -1) {
				executePaginated(nextPage);
			} else {
				executeNext();
			}
		}

		protected abstract void executePaginated(int nextPage);

		protected abstract void executeNext();

		protected abstract void response(T t);

		public abstract void execute();

		@Override
		public void failure(RetrofitError error) {
			if (getOnResultCallback() != null) {
				getOnResultCallback().onFail(error);
			}
		}
	}

	@Nullable
	@Override
	protected InterceptingListOkClient getInterceptor() {
		return new InterceptingListOkClient(new OkHttpClient(), this);
	}
}
