package com.raf.zuhoo.ui.support;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.raf.zuhoo.R;
import com.raf.zuhoo.ui.common.BottomNavActivity;
import com.raf.zuhoo.data.model.response.PageResponse;
import com.raf.zuhoo.data.model.response.SupportTicketResponse;
import com.raf.zuhoo.data.repository.SupportTicketRepository;
import com.raf.zuhoo.databinding.ActivitySupportTicketListBinding;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SupportTicketListActivity extends BottomNavActivity {

    private ActivitySupportTicketListBinding binding;
    private SupportTicketRepository supportTicketRepository;
    private SupportTicketAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySupportTicketListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        supportTicketRepository = new SupportTicketRepository(this);

        adapter = new SupportTicketAdapter(new ArrayList<>(), this::openDetail);
        binding.ticketsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.ticketsRecyclerView.setAdapter(adapter);

        binding.btnNewTicket.setOnClickListener(v ->
                startActivity(new Intent(this, CreateSupportTicketActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTickets();
    }

    private void loadTickets() {

        binding.stateView.showLoading();

        supportTicketRepository.getMyTickets(new Callback<PageResponse<SupportTicketResponse>>() {

            @Override
            public void onResponse(Call<PageResponse<SupportTicketResponse>> call,
                                   Response<PageResponse<SupportTicketResponse>> response) {

                if (!response.isSuccessful() || response.body() == null) {
                    binding.stateView.showContent();
                    Toast.makeText(SupportTicketListActivity.this,
                            R.string.error_tickets_load_failed, Toast.LENGTH_LONG).show();
                    return;
                }

                List<SupportTicketResponse> tickets = response.body().getContent();
                adapter.submitList(tickets);
                if (tickets.isEmpty()) {
                    binding.stateView.showEmpty(R.drawable.ic_ticket,
                            R.string.empty_tickets, R.string.empty_tickets_subtitle);
                } else {
                    binding.stateView.showContent();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<SupportTicketResponse>> call, Throwable t) {
                binding.stateView.showContent();
                Toast.makeText(SupportTicketListActivity.this,
                        R.string.error_tickets_load_failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openDetail(SupportTicketResponse ticket) {
        Intent intent = new Intent(this, SupportTicketDetailActivity.class);
        intent.putExtra(SupportTicketDetailActivity.EXTRA_TICKET_ID, ticket.getId());
        startActivity(intent);
    }
    @Override
    protected int selectedNavItemId() {
        return R.id.nav_support;
    }
}
