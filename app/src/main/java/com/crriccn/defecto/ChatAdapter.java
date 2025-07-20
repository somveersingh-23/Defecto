package com.crriccn.defecto;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<MessageModel> messageList;

    public ChatAdapter(List<MessageModel> messageList) {
        this.messageList = messageList;
    }

    @Override
    public int getItemViewType(int position) {
        return messageList.get(position).getSender();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == MessageModel.USER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_user, parent, false);
            return new UserViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_bot, parent, false);
            return new BotViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        MessageModel message = messageList.get(position);
        if (holder.getItemViewType() == MessageModel.USER) {
            ((UserViewHolder) holder).userMsg.setText(message.getMessage());
        } else {
            ((BotViewHolder) holder).botMsg.setText(message.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView userMsg;
        UserViewHolder(View itemView) {
            super(itemView);
            userMsg = itemView.findViewById(R.id.userMessageTextView);
        }
    }

    static class BotViewHolder extends RecyclerView.ViewHolder {
        TextView botMsg;
        BotViewHolder(View itemView) {
            super(itemView);
            botMsg = itemView.findViewById(R.id.botMessageTextView);
        }
    }
}
