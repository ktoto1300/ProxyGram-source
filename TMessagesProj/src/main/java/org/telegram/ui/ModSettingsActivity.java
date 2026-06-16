package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextSettingsCell;
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
    private int noSponsorRow;
    private int confirmCallRow;
    private int forceMtproto2Row;
    private int hidePhoneRow;
    private int allowScreenshotsRow;
    private int proxyAutoUpdateRow;
    private int proxyStartupLoadingRow;
    private int proxyUpdateIntervalRow;
    private int premiumRow;
    private int liveDraftsRow;
    private int rowCount;

    private boolean isRu() {
        try {
            String lang = LocaleController.getInstance().getCurrentLocaleInfo().shortName;
            return "ru".equals(lang) || "uk".equals(lang) || "be".equals(lang);
        } catch (Exception e) {
            return false;
        }
    }

    private String s(String en, String ru) {
        return isRu() ? ru : en;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(s("ProxyGram Settings", "\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438 ProxyGram"));

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
            } else if (position == noSponsorRow) {
                SharedConfig.noSponsor = !SharedConfig.noSponsor;
                if (SharedConfig.noSponsor) {
                    MessagesController.getInstance(currentAccount).checkPromoInfo(true);
                }
            } else if (position == confirmCallRow) {
                SharedConfig.confirmCall = !SharedConfig.confirmCall;
            } else if (position == forceMtproto2Row) {
                SharedConfig.forceMtproto2 = !SharedConfig.forceMtproto2;
            } else if (position == hidePhoneRow) {
                SharedConfig.hidePhone = !SharedConfig.hidePhone;
            } else if (position == allowScreenshotsRow) {
                SharedConfig.allowScreenshotsInSecret = !SharedConfig.allowScreenshotsInSecret;
            } else if (position == proxyAutoUpdateRow) {
                SharedConfig.proxyAutoUpdate = !SharedConfig.proxyAutoUpdate;
            } else if (position == proxyStartupLoadingRow) {
                SharedConfig.proxyStartupLoading = !SharedConfig.proxyStartupLoading;
            } else if (position == proxyUpdateIntervalRow) {
                org.telegram.ui.ActionBar.AlertDialog.Builder builder = new org.telegram.ui.ActionBar.AlertDialog.Builder(getParentActivity());
                builder.setTitle(s("Update Interval (minutes)", "\u0418\u043d\u0442\u0435\u0440\u0432\u0430\u043b (\u0432 \u043c\u0438\u043d\u0443\u0442\u0430\u0445)"));
                final android.widget.EditText editText = new android.widget.EditText(getParentActivity());
                editText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                editText.setText(String.valueOf(SharedConfig.proxyUpdateInterval));
                builder.setView(editText);
                builder.setPositiveButton(s("Save", "\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c"), (dialog, which) -> {
                    try {
                        SharedConfig.proxyUpdateInterval = Integer.parseInt(editText.getText().toString());
                        SharedConfig.saveConfig();
                        listAdapter.notifyItemChanged(proxyUpdateIntervalRow);
                    } catch (Exception ignore) {}
                });
                builder.setNegativeButton(s("Cancel", "\u041e\u0442\u043c\u0435\u043d\u0430"), null);
                showDialog(builder.create());
                return;
            } else if (position == premiumRow) {
                org.telegram.ui.ActionBar.AlertDialog.Builder builder = new org.telegram.ui.ActionBar.AlertDialog.Builder(getParentActivity());
                builder.setTitle("ProxyGram Premium");
                String msg = isRu()
                    ? "\u2728 \u041f\u0440\u0435\u0438\u043c\u0443\u0449\u0435\u0441\u0442\u0432\u0430 Premium:\n\n\u2022 \u041f\u0440\u0438\u0432\u0430\u0442\u043d\u044b\u0439 \u043d\u0435\u0431\u043b\u043e\u043a\u0438\u0440\u0443\u0435\u043c\u044b\u0439 \u043f\u0440\u043e\u043a\u0441\u0438\n\u2022 \u041f\u043e\u043b\u043d\u043e\u0435 \u043e\u0442\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u0435 \u0440\u0435\u043a\u043b\u0430\u043c\u044b\n\u2022 \u041f\u0440\u0438\u043e\u0440\u0438\u0442\u0435\u0442\u043d\u0430\u044f \u043f\u043e\u0434\u0434\u0435\u0440\u0436\u043a\u0430\n\u2022 \u0411\u044b\u0441\u0442\u0440\u044b\u0435 \u0441\u0435\u0440\u0432\u0435\u0440\u044b\n\n\u0412\u0430\u0448 ID: " + currentUserId
                    : "\u2728 Premium Benefits:\n\n\u2022 Private unblockable proxy\n\u2022 Full ad removal\n\u2022 Priority support\n\u2022 Fastest servers\n\nYour ID: " + currentUserId;
                builder.setMessage(msg);
                builder.setPositiveButton(isRu() ? "\u041a\u0443\u043f\u0438\u0442\u044c" : "Buy", (dialog, which) -> {
                    org.telegram.messenger.browser.Browser.openUrl(getParentActivity(), "https://t.me/ktoto13000");
                });
                builder.setNegativeButton(isRu() ? "\u0417\u0430\u043a\u0440\u044b\u0442\u044c" : "Close", null);
                showDialog(builder.create());
                return;
            } else if (position == liveDraftsRow) {
                if (SharedConfig.isPremium()) {
                    SharedConfig.liveDrafts = !SharedConfig.liveDrafts;
                    SharedConfig.saveConfig();
                    if (view instanceof TextCheckCell) {
                        ((TextCheckCell) view).setChecked(SharedConfig.liveDrafts);
                    }
                } else {
                    org.telegram.ui.ActionBar.AlertDialog.Builder builder = new org.telegram.ui.ActionBar.AlertDialog.Builder(getParentActivity());
                    builder.setTitle("ProxyGram Premium");
                    String msg = isRu()
                        ? "\u2728 \u041f\u0440\u0435\u0438\u043c\u0443\u0449\u0435\u0441\u0442\u0432\u0430 Premium:\n\n\u2022 \u041f\u0440\u0438\u0432\u0430\u0442\u043d\u044b\u0439 \u043d\u0435\u0431\u043b\u043e\u043a\u0438\u0440\u0443\u0435\u043c\u044b\u0439 \u043f\u0440\u043e\u043a\u0441\u0438\n\u2022 \u041f\u043e\u043b\u043d\u043e\u0435 \u043e\u0442\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u0435 \u0440\u0435\u043a\u043b\u0430\u043c\u044b\n\u2022 \u041f\u0440\u0438\u043e\u0440\u0438\u0442\u0435\u0442\u043d\u0430\u044f \u043f\u043e\u0434\u0434\u0435\u0440\u0436\u043a\u0430\n\u2022 \u0411\u044b\u0441\u0442\u0440\u044b\u0435 \u0441\u0435\u0440\u0432\u0435\u0440\u044b\n\u2022 \u0411\u044b\u0441\u0442\u0440\u044b\u0435 \u0447\u0435\u0440\u043d\u043e\u0432\u0438\u043a\u0438 (Live Drafts)\n\n\u0412\u0430\u0448 ID: " + currentUserId
                        : "\u2728 Premium Benefits:\n\n\u2022 Private unblockable proxy\n\u2022 Full ad removal\n\u2022 Priority support\n\u2022 Fastest servers\n\u2022 Live Drafts\n\nYour ID: " + currentUserId;
                    builder.setMessage(msg);
                    builder.setPositiveButton(isRu() ? "\u041a\u0443\u043f\u0438\u0442\u044c" : "Buy", (dialog, which) -> {
                        org.telegram.messenger.browser.Browser.openUrl(getParentActivity(), "https://t.me/ktoto13000");
                    });
                    builder.setNegativeButton(isRu() ? "\u0417\u0430\u043a\u0440\u044b\u0442\u044c" : "Close", null);
                    showDialog(builder.create());
                }
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
                title = s("Ghost Mode", "\u0420\u0435\u0436\u0438\u043c \u041d\u0435\u0432\u0438\u0434\u0438\u043c\u043a\u0438");
                infoText = s("When enabled, your status appears as offline.", "\u0421\u043a\u0440\u044b\u0432\u0430\u0435\u0442 \u0432\u0430\u0448 \u043e\u043d\u043b\u0430\u0439\u043d-\u0441\u0442\u0430\u0442\u0443\u0441.");
            } else if (position == hideOnlineRow) {
                title = s("Hide Typing Status", "\u0421\u043a\u0440\u044b\u0442\u044c '\u041f\u0435\u0447\u0430\u0442\u0430\u0435\u0442...'");
                infoText = s("Hides your typing and online status.", "\u041f\u043e\u043b\u043d\u043e\u0441\u0442\u044c\u044e \u0441\u043a\u0440\u044b\u0432\u0430\u0435\u0442 \u0441\u0442\u0430\u0442\u0443\u0441 \u043d\u0430\u0431\u043e\u0440\u0430 \u0442\u0435\u043a\u0441\u0442\u0430.");
            } else if (position == antiDeleteRow) {
                title = s("Anti-Delete Messages", "\u0410\u043d\u0442\u0438-\u0423\u0434\u0430\u043b\u0435\u043d\u0438\u0435");
                infoText = s("Deleted messages marked with Deleted.", "\u0423\u0434\u0430\u043b\u0451\u043d\u043d\u044b\u0435 \u0441\u043e\u043e\u0431\u0449\u0435\u043d\u0438\u044f \u043f\u043e\u043c\u0435\u0447\u0430\u044e\u0442\u0441\u044f '\u0423\u0434\u0430\u043b\u0435\u043d\u043e'.");
            } else if (position == antiViewOnceRow) {
                title = "Anti-View Once";
                infoText = s("Disappearing media will not be deleted.", "\u0418\u0441\u0447\u0435\u0437\u0430\u044e\u0449\u0435\u0435 \u043c\u0435\u0434\u0438\u0430 \u043d\u0435 \u0431\u0443\u0434\u0435\u0442 \u0443\u0434\u0430\u043b\u044f\u0442\u044c\u0441\u044f.");
            } else if (position == noAdsRow) {
                title = s("No Ads", "\u0411\u0435\u0437 \u0440\u0435\u043a\u043b\u0430\u043c\u044b");
                infoText = s("Removes sponsored messages.", "\u0423\u0434\u0430\u043b\u044f\u0435\u0442 \u0441\u043f\u043e\u043d\u0441\u043e\u0440\u0441\u043a\u0438\u0435 \u043f\u043e\u0441\u0442\u044b \u0432 \u043a\u0430\u043d\u0430\u043b\u0430\u0445.");
            } else if (position == noSponsorRow) {
                title = s("Block Proxy Sponsors", "\u0421\u043a\u0440\u044b\u0442\u044c \u0441\u043f\u043e\u043d\u0441\u043e\u0440\u043e\u0432 \u043f\u0440\u043e\u043a\u0441\u0438");
                infoText = s("Hides sponsored channels from MTProto proxies.", "\u0423\u0431\u0438\u0440\u0430\u0435\u0442 \u043a\u0430\u043d\u0430\u043b\u044b-\u0441\u043f\u043e\u043d\u0441\u043e\u0440\u044b \u043e\u0442 MTProto \u043f\u0440\u043e\u043a\u0441\u0438.");
            } else if (position == confirmCallRow) {
                title = s("Confirm Before Call", "\u041f\u043e\u0434\u0442\u0432\u0435\u0440\u0436\u0434\u0435\u043d\u0438\u0435 \u0437\u0432\u043e\u043d\u043a\u0430");
                infoText = s("Shows a confirmation dialog before a call.", "\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0435\u0442 \u0434\u0438\u0430\u043b\u043e\u0433 \u043f\u0435\u0440\u0435\u0434 \u043d\u0430\u0447\u0430\u043b\u043e\u043c \u0437\u0432\u043e\u043d\u043a\u0430.");
            } else if (position == forceMtproto2Row) {
                title = s("Forced MTProto 2.0", "\u041f\u0440\u0438\u043d\u0443\u0434\u0438\u0442\u0435\u043b\u044c\u043d\u044b\u0439 MTProto 2.0");
                infoText = s("Prepends ee to MTProto proxy secrets.", "\u0414\u043e\u0431\u0430\u0432\u043b\u044f\u0435\u0442 ee \u043f\u0435\u0440\u0435\u0434 \u0441\u0435\u043a\u0440\u0435\u0442\u043e\u043c \u043f\u0440\u043e\u043a\u0441\u0438.");
            } else if (position == hidePhoneRow) {
                title = s("Hide Phone Number", "\u0421\u043a\u0440\u044b\u0442\u044c \u043d\u043e\u043c\u0435\u0440 \u0442\u0435\u043b\u0435\u0444\u043e\u043d\u0430");
                infoText = s("Replaces your phone number with *.", "\u0412\u043c\u0435\u0441\u0442\u043e \u043d\u043e\u043c\u0435\u0440\u0430 \u043e\u0442\u043e\u0431\u0440\u0430\u0436\u0430\u0435\u0442\u0441\u044f *.");
            } else if (position == allowScreenshotsRow) {
                title = s("Allow Screenshots", "\u0420\u0430\u0437\u0440\u0435\u0448\u0438\u0442\u044c \u0441\u043a\u0440\u0438\u043d\u0448\u043e\u0442\u044b");
                infoText = s("Bypasses screenshot restrictions.", "\u041f\u043e\u0437\u0432\u043e\u043b\u044f\u0435\u0442 \u0434\u0435\u043b\u0430\u0442\u044c \u0441\u043a\u0440\u0438\u043d\u0448\u043e\u0442\u044b \u0432 \u0441\u0435\u043a\u0440\u0435\u0442\u043d\u044b\u0445 \u0447\u0430\u0442\u0430\u0445.");
            } else if (position == proxyAutoUpdateRow) {
                title = s("Auto-Update Proxies", "\u0410\u0432\u0442\u043e\u043e\u0431\u043d\u043e\u0432\u043b\u0435\u043d\u0438\u0435 \u043f\u0440\u043e\u043a\u0441\u0438");
                infoText = s("Automatically updates proxy list.", "\u0410\u0432\u0442\u043e\u043c\u0430\u0442\u0438\u0447\u0435\u0441\u043a\u0438 \u043e\u0431\u043d\u043e\u0432\u043b\u044f\u0435\u0442 \u0441\u043f\u0438\u0441\u043e\u043a \u043f\u0440\u043e\u043a\u0441\u0438.");
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
            proxyStartupLoadingRow = rowCount++;
            proxyUpdateIntervalRow = rowCount++;
            premiumRow = rowCount++;
            liveDraftsRow = rowCount++;
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
            View view;
            if (viewType == 2) {
                view = new TextSettingsCell(mContext);
            } else {
                view = new TextCheckCell(mContext);
            }
            view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder.getItemViewType() == 1) {
                TextCheckCell checkCell = (TextCheckCell) holder.itemView;
                if (position == ghostModeRow) {
                    checkCell.setTextAndCheck(s("Ghost Mode", "\u0420\u0435\u0436\u0438\u043c \u041d\u0435\u0432\u0438\u0434\u0438\u043c\u043a\u0438"), SharedConfig.ghostMode, true);
                } else if (position == hideOnlineRow) {
                    checkCell.setTextAndCheck(s("Hide Typing Status", "\u0421\u043a\u0440\u044b\u0442\u044c '\u041f\u0435\u0447\u0430\u0442\u0430\u0435\u0442...'"), SharedConfig.hideOnline, true);
                } else if (position == antiDeleteRow) {
                    checkCell.setTextAndCheck(s("Anti-Delete Messages", "\u0410\u043d\u0442\u0438-\u0423\u0434\u0430\u043b\u0435\u043d\u0438\u0435"), SharedConfig.saveDeleted, true);
                } else if (position == antiViewOnceRow) {
                    checkCell.setTextAndCheck(s("Anti-View Once", "Anti-View Once"), SharedConfig.saveViewOnce, true);
                } else if (position == noAdsRow) {
                    checkCell.setTextAndCheck(s("No Ads", "\u0411\u0435\u0437 \u0440\u0435\u043a\u043b\u0430\u043c\u044b"), SharedConfig.noAds, true);
                } else if (position == noSponsorRow) {
                    checkCell.setTextAndCheck(s("Block Proxy Sponsors", "\u0421\u043a\u0440\u044b\u0442\u044c \u0441\u043f\u043e\u043d\u0441\u043e\u0440\u043e\u0432 \u043f\u0440\u043e\u043a\u0441\u0438"), SharedConfig.noSponsor, true);
                } else if (position == confirmCallRow) {
                    checkCell.setTextAndCheck(s("Confirm Before Call", "\u041f\u043e\u0434\u0442\u0432\u0435\u0440\u0436\u0434\u0435\u043d\u0438\u0435 \u0437\u0432\u043e\u043d\u043a\u0430"), SharedConfig.confirmCall, true);
                } else if (position == forceMtproto2Row) {
                    checkCell.setTextAndCheck(s("Forced MTProto 2.0", "\u041f\u0440\u0438\u043d\u0443\u0434. MTProto 2.0"), SharedConfig.forceMtproto2, true);
                } else if (position == hidePhoneRow) {
                    checkCell.setTextAndCheck(s("Hide Phone Number", "\u0421\u043a\u0440\u044b\u0442\u044c \u043d\u043e\u043c\u0435\u0440"), SharedConfig.hidePhone, true);
                } else if (position == allowScreenshotsRow) {
                    checkCell.setTextAndCheck(s("Allow Screenshots", "\u0420\u0430\u0437\u0440\u0435\u0448\u0438\u0442\u044c \u0441\u043a\u0440\u0438\u043d\u0448\u043e\u0442\u044b"), SharedConfig.allowScreenshotsInSecret, true);
                } else if (position == proxyAutoUpdateRow) {
                    checkCell.setTextAndCheck(s("Auto-Update Proxies", "\u0410\u0432\u0442\u043e\u043e\u0431\u043d. \u043f\u0440\u043e\u043a\u0441\u0438"), SharedConfig.proxyAutoUpdate, true);
                } else if (position == proxyStartupLoadingRow) {
                    checkCell.setTextAndCheck(s("Check Proxies on Startup", "\u041f\u0440\u043e\u0432\u0435\u0440\u043a\u0430 \u043f\u0440\u043e\u043a\u0441\u0438 \u043f\u0440\u0438 \u0432\u0445\u043e\u0434\u0435"), SharedConfig.proxyStartupLoading, true);
                } else if (position == liveDraftsRow) {
                    checkCell.setTextAndCheck(s("Live Drafts (Premium)", "\u0411\u044b\u0441\u0442\u0440\u044b\u0435 \u0447\u0435\u0440\u043d\u043e\u0432\u0438\u043a\u0438 (Premium)"), SharedConfig.liveDrafts, true);
                }
            } else if (holder.getItemViewType() == 2) {
                TextSettingsCell settingsCell = (TextSettingsCell) holder.itemView;
                if (position == proxyUpdateIntervalRow) {
                    settingsCell.setTextAndValue(s("Interval: ", "\u0418\u043d\u0442\u0435\u0440\u0432\u0430\u043b: ") + SharedConfig.proxyUpdateInterval + s(" min", " \u043c\u0438\u043d"), "", true);
                } else if (position == premiumRow) {
                    settingsCell.setText("ProxyGram Premium", false);
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == proxyUpdateIntervalRow || position == premiumRow) {
                return 2;
            }
            return 1;
        }
    }
}
