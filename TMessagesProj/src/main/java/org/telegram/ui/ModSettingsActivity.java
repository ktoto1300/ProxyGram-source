package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.UserConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.MessagesController;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

public class ModSettingsActivity extends BaseFragment {

    private ListAdapter listAdapter;
    private RecyclerListView listView;

    private int ghostModeRow;
    private int hideOnlineRow;
    private int antiDeleteRow;
    private int antiViewOnceRow;
    private int noAdsRow;
    private int confirmCallRow;
    private int forceMtproto2Row;
    private int hidePhoneRow;
    private int allowScreenshotsRow;
    private int proxyAutoUpdateRow;
    private int proxyUpdateIntervalRow;
    private int noSponsorRow;
    private int premiumRow;
    private int rowCount;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("ProxyGramSettings", R.string.ProxyGramSettings));

        
        long currentUserId = UserConfig.getInstance(currentAccount).getClientUserId();
        
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        listAdapter = new ListAdapter(context);

        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        listView.setAdapter(listAdapter);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setOnItemClickListener((view, position) -> {
            if (position == ghostModeRow) {
                SharedConfig.ghostMode = !SharedConfig.ghostMode;
            } else if (position == hideOnlineRow) {
                SharedConfig.hideOnline = !SharedConfig.hideOnline;
            } else if (position == antiDeleteRow) {
                SharedConfig.saveDeleted = !SharedConfig.saveDeleted;
            } else if (position == antiViewOnceRow) {
                SharedConfig.saveViewOnce = !SharedConfig.saveViewOnce;
            } else if (position == noAdsRow) {
                SharedConfig.noAds = !SharedConfig.noAds;
            } else if (position == confirmCallRow) {
                SharedConfig.confirmCall = !SharedConfig.confirmCall;
            } else if (position == forceMtproto2Row) {
                SharedConfig.forceMtproto2 = !SharedConfig.forceMtproto2;
            } else if (position == hidePhoneRow) {
                SharedConfig.hidePhone = !SharedConfig.hidePhone;
            } else if (position == allowScreenshotsRow) {
                SharedConfig.allowScreenshotsInSecret = !SharedConfig.allowScreenshotsInSecret;
            } else if (position == noSponsorRow) {
                SharedConfig.noSponsor = !SharedConfig.noSponsor;
                if (SharedConfig.noSponsor) {
                    MessagesController.getInstance(currentAccount).checkPromoInfo(true);
                }
            } else if (position == proxyAutoUpdateRow) {
                SharedConfig.proxyAutoUpdate = !SharedConfig.proxyAutoUpdate;
            } else if (position == proxyUpdateIntervalRow) {
                org.telegram.ui.ActionBar.AlertDialog.Builder builder = new org.telegram.ui.ActionBar.AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString("UpdateIntervalTitle", R.string.UpdateIntervalTitle));
                final android.widget.EditText editText = new android.widget.EditText(getParentActivity());
                editText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                editText.setText(String.valueOf(SharedConfig.proxyUpdateInterval));
                builder.setView(editText);
                builder.setPositiveButton("Save", (dialog, which) -> {
                    try {
                        SharedConfig.proxyUpdateInterval = Integer.parseInt(editText.getText().toString());
                        SharedConfig.saveConfig();
                        listAdapter.notifyItemChanged(proxyUpdateIntervalRow);
                    } catch (Exception ignore) {}
                });
                builder.setNegativeButton("Cancel", null);
                showDialog(builder.create());
                return;
            } else if (position == premiumRow) {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("User ID", String.valueOf(currentUserId));
                clipboard.setPrimaryClip(clip);
                android.widget.Toast.makeText(context, "ID copied: " + currentUserId, android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            SharedConfig.saveConfig();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(!((TextCheckCell) view).isChecked());
            }
        });

        listView.setOnItemLongClickListener((view, position) -> {
            String infoText = "";
            String title = "";
            if (position == ghostModeRow) {
                title = LocaleController.getString("GhostMode", R.string.GhostMode);
                infoText = LocaleController.getString("GhostModeInfo", R.string.GhostModeInfo);
            } else if (position == hideOnlineRow) {
                title = LocaleController.getString("HideOnlineStatus", R.string.HideOnlineStatus);
                infoText = LocaleController.getString("HideOnlineStatusInfo", R.string.HideOnlineStatusInfo);
            } else if (position == antiDeleteRow) {
                title = LocaleController.getString("AntiDeleteMessages", R.string.AntiDeleteMessages);
                infoText = LocaleController.getString("AntiDeleteMessagesInfo", R.string.AntiDeleteMessagesInfo);
            } else if (position == antiViewOnceRow) {
                title = LocaleController.getString("AntiViewOnce", R.string.AntiViewOnce);
                infoText = LocaleController.getString("AntiViewOnceInfo", R.string.AntiViewOnceInfo);
            } else if (position == noAdsRow) {
                title = LocaleController.getString("NoAds", R.string.NoAds);
                infoText = LocaleController.getString("NoAdsInfo", R.string.NoAdsInfo);
            } else if (position == noSponsorRow) {
                title = LocaleController.getString("BlockProxySponsors", R.string.BlockProxySponsors);
                infoText = LocaleController.getString("BlockProxySponsorsInfo", R.string.BlockProxySponsorsInfo);
            } else if (position == confirmCallRow) {
                title = LocaleController.getString("ConfirmBeforeCall", R.string.ConfirmBeforeCall);
                infoText = LocaleController.getString("ConfirmBeforeCallInfo", R.string.ConfirmBeforeCallInfo);
            } else if (position == forceMtproto2Row) {
                title = LocaleController.getString("ForcedMTProto2", R.string.ForcedMTProto2);
                infoText = LocaleController.getString("ForcedMTProto2Info", R.string.ForcedMTProto2Info);
            } else if (position == hidePhoneRow) {
                title = LocaleController.getString("HideMyPhoneNumber", R.string.HideMyPhoneNumber);
                infoText = LocaleController.getString("HideMyPhoneNumberInfo", R.string.HideMyPhoneNumberInfo);
            } else if (position == allowScreenshotsRow) {
                title = LocaleController.getString("AllowScreenshots", R.string.AllowScreenshots);
                infoText = LocaleController.getString("AllowScreenshotsInfo", R.string.AllowScreenshotsInfo);
            } else if (position == proxyAutoUpdateRow) {
                title = LocaleController.getString("AutoUpdateProxies", R.string.AutoUpdateProxies);
                infoText = LocaleController.getString("AutoUpdateProxiesInfo", R.string.AutoUpdateProxiesInfo);
            }

            if (!infoText.isEmpty()) {
                org.telegram.ui.ActionBar.AlertDialog.Builder builder = new org.telegram.ui.ActionBar.AlertDialog.Builder(getParentActivity());
                builder.setTitle(title);
                builder.setMessage(infoText);
                builder.setPositiveButton("OK", null);
                showDialog(builder.create());
                return true;
            }
            return false;
        });

        return fragmentView;
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
            updateRows();
        }

        private void updateRows() {
            rowCount = 0;
            ghostModeRow = rowCount++;
            hideOnlineRow = rowCount++;
            antiDeleteRow = rowCount++;
            antiViewOnceRow = rowCount++;
            noAdsRow = rowCount++;
            noSponsorRow = rowCount++;
            confirmCallRow = rowCount++;
            forceMtproto2Row = rowCount++;
            hidePhoneRow = rowCount++;
            allowScreenshotsRow = rowCount++;
            proxyAutoUpdateRow = rowCount++;
            proxyUpdateIntervalRow = rowCount++;
            premiumRow = rowCount++;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = new TextCheckCell(mContext);
            view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            TextCheckCell checkCell = (TextCheckCell) holder.itemView;
            if (position == ghostModeRow) {
                checkCell.setTextAndCheck(LocaleController.getString("GhostMode", R.string.GhostMode), SharedConfig.ghostMode, true);
            } else if (position == hideOnlineRow) {
                checkCell.setTextAndCheck(LocaleController.getString("HideOnlineStatus", R.string.HideOnlineStatus), SharedConfig.hideOnline, true);
            } else if (position == antiDeleteRow) {
                checkCell.setTextAndCheck(LocaleController.getString("AntiDeleteMessages", R.string.AntiDeleteMessages), SharedConfig.saveDeleted, true);
            } else if (position == antiViewOnceRow) {
                checkCell.setTextAndCheck(LocaleController.getString("AntiViewOnce", R.string.AntiViewOnce), SharedConfig.saveViewOnce, true);
            } else if (position == noAdsRow) {
                checkCell.setTextAndCheck(LocaleController.getString("NoAds", R.string.NoAds), SharedConfig.noAds, true);
            } else if (position == noSponsorRow) {
                checkCell.setTextAndCheck(LocaleController.getString("BlockProxySponsors", R.string.BlockProxySponsors), SharedConfig.noSponsor, true);
            } else if (position == confirmCallRow) {
                checkCell.setTextAndCheck(LocaleController.getString("ConfirmBeforeCall", R.string.ConfirmBeforeCall), SharedConfig.confirmCall, true);
            } else if (position == forceMtproto2Row) {
                checkCell.setTextAndCheck(LocaleController.getString("ForcedMTProto2", R.string.ForcedMTProto2), SharedConfig.forceMtproto2, true);
            } else if (position == hidePhoneRow) {
                checkCell.setTextAndCheck(LocaleController.getString("HideMyPhoneNumber", R.string.HideMyPhoneNumber), SharedConfig.hidePhone, true);
            } else if (position == allowScreenshotsRow) {
                checkCell.setTextAndCheck(LocaleController.getString("AllowScreenshots", R.string.AllowScreenshots), SharedConfig.allowScreenshotsInSecret, true);
            } else if (position == proxyAutoUpdateRow) {
                checkCell.setTextAndCheck(LocaleController.getString("AutoUpdateProxies", R.string.AutoUpdateProxies), SharedConfig.proxyAutoUpdate, true);
            } else if (position == proxyUpdateIntervalRow) {
                checkCell.setTextAndCheck(LocaleController.formatString("UpdateInterval", R.string.UpdateInterval, SharedConfig.proxyUpdateInterval), false, true);
            } else if (position == premiumRow) {
                checkCell.setTextAndCheck(LocaleController.formatString("YourID", R.string.YourID, UserConfig.getInstance(currentAccount).getClientUserId()), false, false);
            }
        }

        @Override
        public int getItemViewType(int position) {
            return 1;
        }
    }
}
