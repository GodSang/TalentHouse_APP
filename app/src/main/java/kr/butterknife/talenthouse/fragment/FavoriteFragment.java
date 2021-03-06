package kr.butterknife.talenthouse.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import kr.butterknife.talenthouse.LoadingDialog;
import kr.butterknife.talenthouse.LoginInfo;
import kr.butterknife.talenthouse.activity.MainActivity;
import kr.butterknife.talenthouse.adapter.MainRVAdapter;
import kr.butterknife.talenthouse.OnItemClickListener;
import kr.butterknife.talenthouse.PostItem;
import kr.butterknife.talenthouse.R;
import kr.butterknife.talenthouse.Util;
import kr.butterknife.talenthouse.network.ButterKnifeApi;
import kr.butterknife.talenthouse.network.response.PostRes;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class FavoriteFragment extends Fragment {

    private RecyclerView rv;
    private MainRVAdapter rvAdapter;
    private ArrayList<PostItem> posts;
    TextView textView;
    private LinearLayout linearLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view  = inflater.inflate(R.layout.fragment_favorite, container, false);

        rv = view.findViewById(R.id.favorite_rv);
        textView = view.findViewById(R.id.favorite_tv_nickname);
        textView.setText(LoginInfo.INSTANCE.getLoginInfo(getContext())[1]);
        linearLayout = view.findViewById(R.id.favorite_ll);
        posts = new ArrayList<>();
        rvAdapter = new MainRVAdapter(getContext(), posts);
        rvAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(@Nullable View v, int pos) {
                ((MainActivity)getActivity()).replaceFragment(
                        new ContentFragment(posts.get(pos)),
                        "Content"
                );
            }
        });
        rvAdapter.setOnSettingListener((v, postId) -> {
            Util.INSTANCE.postSetting(requireContext(), v, postId, posts, (item) -> {
                ((MainActivity) getActivity()).replaceFragment(new WriteFragment(), "Write", item);
                return true;
            }, (idx) -> {
                posts.remove((int) idx);
                rvAdapter.notifyDataSetChanged();
                return true;
            });
        });
        rvAdapter.initScrollListener(rv);
        rvAdapter.setOnItemReloadListener(() -> getFavoritePosts());

        rv.setAdapter(rvAdapter);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        rvAdapter.doItemReload();
        return view;
    }

    public void getFavoritePosts(){
        new Runnable(){
            @Override
            public void run() {
                try{
                    LoadingDialog.INSTANCE.onLoadingDialog(getActivity());
                    String userId = LoginInfo.INSTANCE.getLoginInfo(getContext())[0];
                    ButterKnifeApi.INSTANCE.getRetrofitService().getFavoritePost(userId, String.valueOf(rvAdapter.getPage())).enqueue(new Callback<PostRes>() {
                        @Override
                        public void onResponse(Call<PostRes> call, Response<PostRes> response) {
                            if(response.body() != null){
                                linearLayout.setVisibility(View.GONE);
                                rv.setVisibility(View.VISIBLE);
                                List<PostItem> postItemList = response.body().getData();
                                if(postItemList.size() != 0){
                                    for(PostItem p : postItemList) {
                                        if(p.getVideoUrl() != null)
                                            posts.add(new PostItem(p.get_id(), p.getTitle(), p.getWriterNickname(), p.getWriterId(), p.getUpdateTime(), p.getDescription(), p.getVideoUrl(), p.getLikeCnt(), p.getLikeIDs(), p.getCategory(), p.getComments(), p.getProfile()));
                                        else if(p.getImageUrl().size() != 0)
                                            posts.add(new PostItem(p.get_id(), p.getTitle(), p.getWriterNickname(), p.getWriterId(), p.getUpdateTime(), p.getDescription(), p.getImageUrl(), p.getLikeCnt(), p.getLikeIDs(), p.getCategory(), p.getComments(), p.getProfile()));
                                        else
                                            posts.add(new PostItem(p.get_id(), p.getTitle(), p.getWriterNickname(), p.getWriterId(), p.getUpdateTime(), p.getDescription(), p.getLikeCnt(), p.getLikeIDs(), p.getCategory(), p.getComments(), p.getProfile()));
                                        rvAdapter.notifyItemInserted(posts.size() - 1);
                                    }
                                }else{
                                    if(posts.size() == 0) {
                                        linearLayout.setVisibility(View.VISIBLE);
                                        rv.setVisibility(View.GONE);
                                    }
                                    rvAdapter.setPage(rvAdapter.getPage() - 1);
                                }
                            }
                            LoadingDialog.INSTANCE.offLoadingDialog();
                        }
                        @Override
                        public void onFailure(Call<PostRes> call, Throwable t) {
                            Log.d("err", "SERVER CONNECTION ERROR");
                            LoadingDialog.INSTANCE.offLoadingDialog();
                        }
                    });
                }
                catch (Exception e){
                    e.printStackTrace();
                    LoadingDialog.INSTANCE.offLoadingDialog();
                }
            }
        }.run();
    }

}