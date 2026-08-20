package com.raf.zuhoo.ui.servicerequest;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.raf.zuhoo.data.model.response.RequestComment;
import com.raf.zuhoo.databinding.ItemCommentBinding;

import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {

    private final List<RequestComment> comments;

    public CommentAdapter(List<RequestComment> comments) {
        this.comments = comments;
    }

    public void submitList(List<RequestComment> newComments) {
        comments.clear();
        comments.addAll(newComments);
        notifyDataSetChanged();
    }

    // Socket-pushed comments can collide with ones already loaded over REST (a reconnect refetch,
    // or a second session of the same user), so match on the server id before appending rather
    // than showing the same message twice.
    public void addComment(RequestComment comment) {

        int existing = indexOf(comment.getId());

        if (existing >= 0) {
            comments.set(existing, comment);
            notifyItemChanged(existing);
            return;
        }

        comments.add(comment);
        notifyItemInserted(comments.size() - 1);
    }

    private int indexOf(Long id) {

        if (id == null) {
            return -1;
        }

        for (int i = 0; i < comments.size(); i++) {
            if (id.equals(comments.get(i).getId())) {
                return i;
            }
        }

        return -1;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCommentBinding binding = ItemCommentBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(comments.get(position));
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemCommentBinding binding;

        ViewHolder(ItemCommentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(RequestComment comment) {
            binding.commentAuthor.setText(comment.getAuthorName());
            binding.commentContent.setText(comment.getContent());
            binding.commentDate.setText(comment.getCreatedAt());
        }
    }
}
