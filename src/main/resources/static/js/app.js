const tg = window.Telegram?.WebApp;
    let initData = '';
    let isAdmin = false;
    let allTournaments = [];
    let currentMainTab = 'tournaments';
    let tvState = {
        tournamentId: null,
        tournamentTitle: '',
        excelUrl: '',
        running: false,
        currentLevel: 1,
        secondsLeft: 20 * 60,
        break: false,
        smallBlind: 100,
        bigBlind: 200,
        ante: 200
    };

    const BLINDS_TABLE = [
        { level: 1,  sb: 100,   bb: 200,    ante: 200,   minutes: 20, isBreak: false },
        { level: 2,  sb: 200,   bb: 400,    ante: 400,   minutes: 20, isBreak: false },
        { level: 3,  sb: 300,   bb: 600,    ante: 600,   minutes: 20, isBreak: false },
        { level: 4,  sb: 400,   bb: 800,    ante: 800,   minutes: 20, isBreak: false },
        { level: 5,  sb: 500,   bb: 1000,   ante: 1000,  minutes: 20, isBreak: false },
        { level: 6,  sb: 0,     bb: 0,      ante: 0,     minutes: 15, isBreak: true  },
        { level: 7,  sb: 600,   bb: 1200,   ante: 1200,  minutes: 20, isBreak: false },
        { level: 8,  sb: 800,   bb: 1600,   ante: 1600,  minutes: 20, isBreak: false },
        { level: 9,  sb: 1000,  bb: 2000,   ante: 2000,  minutes: 20, isBreak: false },
        { level: 10, sb: 1200,  bb: 2400,   ante: 2400,  minutes: 20, isBreak: false },
        { level: 11, sb: 1500,  bb: 3000,   ante: 3000,  minutes: 20, isBreak: false },
        { level: 12, sb: 0,     bb: 0,      ante: 0,     minutes: 15, isBreak: true  },
        { level: 13, sb: 2000,  bb: 4000,   ante: 4000,  minutes: 20, isBreak: false },
        { level: 14, sb: 2500,  bb: 5000,   ante: 5000,  minutes: 20, isBreak: false },
        { level: 15, sb: 3000,  bb: 6000,   ante: 6000,  minutes: 20, isBreak: false },
        { level: 16, sb: 4000,  bb: 8000,   ante: 8000,  minutes: 20, isBreak: false },
        { level: 17, sb: 5000,  bb: 10000,  ante: 10000, minutes: 20, isBreak: false },
        { level: 18, sb: 0,     bb: 0,      ante: 0,     minutes: 15, isBreak: true  },
        { level: 19, sb: 6000,  bb: 12000,  ante: 12000, minutes: 20, isBreak: false },
        { level: 20, sb: 8000,  bb: 16000,  ante: 16000, minutes: 20, isBreak: false },
        { level: 21, sb: 10000, bb: 20000,  ante: 20000, minutes: 20, isBreak: false },
        { level: 22, sb: 15000, bb: 30000,  ante: 30000, minutes: 20, isBreak: false },
        { level: 23, sb: 20000, bb: 40000,  ante: 40000, minutes: 20, isBreak: false },
        { level: 24, sb: 25000, bb: 50000,  ante: 50000, minutes: 20, isBreak: false },
        { level: 25, sb: 30000, bb: 60000,  ante: 60000, minutes: 20, isBreak: false },
        { level: 26, sb: 40000, bb: 80000,  ante: 80000, minutes: 20, isBreak: false },
        { level: 27, sb: 50000, bb: 100000, ante: 100000,minutes: 20, isBreak: false }
    ];

    const mockTournaments = [
        {
            id: 1,
            title: "Texas Hold'em Main Event",
            excelUrl: "/demo_tournament.xlsx",
            startsAt: new Date(Date.now() + 86400000).toISOString(),
            maxPlayers: 24,
            participantsCount: 16,
            registered: true,
            finished: false
        },
        {
            id: 2,
            title: "Sunday Bounty Knockout",
            excelUrl: "https://docs.google.com/spreadsheets/d/1jBKowXMyGl0dx_QdYaXW_x9-EB4Ey7-v1KlN4KXKGUc/edit",
            startsAt: new Date(Date.now() + 172800000).toISOString(),
            maxPlayers: 18,
            participantsCount: 9,
            registered: false,
            finished: false
        },
        {
            id: 3,
            title: "Omaha High Roller Deepstack",
            excelUrl: "/demo_tournament.xlsx",
            startsAt: new Date(Date.now() - 3600000).toISOString(),
            maxPlayers: 30,
            participantsCount: 14,
            registered: false,
            finished: true
        }
    ];

    function sortTournaments(tournaments) {
        return (tournaments || []).slice().sort((a, b) => {
            const timeA = a.startsAt ? new Date(a.startsAt).getTime() : Infinity;
            const timeB = b.startsAt ? new Date(b.startsAt).getTime() : Infinity;
            if (timeA !== timeB) return timeA - timeB;
            return (a.id || 0) - (b.id || 0);
        });
    }

    function triggerHaptic(type = 'light') {
        try {
            if (tg?.HapticFeedback) {
                if (type === 'success' || type === 'error' || type === 'warning') {
                    tg.HapticFeedback.notificationOccurred(type);
                } else {
                    tg.HapticFeedback.impactOccurred(type);
                }
            }
        } catch (e) {}
    }

    let toastTimer = null;
    function showToast(msg, type = 'info') {
        const toast = document.getElementById('toast');
        const toastMsg = document.getElementById('toastMsg');
        toastMsg.textContent = msg;
        toast.className = 'toast show';

        if (type === 'success') {
            toast.style.borderColor = 'var(--green-border)';
            triggerHaptic('success');
        } else if (type === 'error') {
            toast.style.borderColor = 'rgba(225, 29, 72, 0.6)';
            triggerHaptic('error');
        } else {
            toast.style.borderColor = 'var(--border-gold)';
            triggerHaptic('medium');
        }

        clearTimeout(toastTimer);
        toastTimer = setTimeout(() => {
            toast.className = 'toast';
        }, 3000);
    }

    if (tg) {
        tg.ready();
        tg.expand();
        try {
            tg.setHeaderColor('#070709');
            tg.setBackgroundColor('#070709');
        } catch (e) {}

        initData = tg.initData || '';
        const user = tg.initDataUnsafe?.user;
        if (user) {
            document.getElementById('userName').textContent = user.first_name || user.username || 'Игрок';
        }
    }

    let currentLeaderboardPeriod = 'season';
    let currentSeason = null;
    let myTgId = null;
    let selectedResultsTourneyId = null;

    function updateAdminUI(adminStatus) {
        isAdmin = !!adminStatus;
        const adminTag = document.getElementById('adminTag');
        if (adminTag) adminTag.style.display = isAdmin ? 'inline-block' : 'none';

        const navTabs = document.getElementById('adminNavTabs');
        if (navTabs) {
            if (isAdmin) navTabs.classList.add('has-admin');
            else navTabs.classList.remove('has-admin');
        }

        const tabBtnAdmin = document.getElementById('tabBtnAdmin');
        if (tabBtnAdmin) tabBtnAdmin.style.display = isAdmin ? 'flex' : 'none';

        const adminSeasonControls = document.getElementById('adminSeasonControls');
        if (adminSeasonControls) adminSeasonControls.style.display = isAdmin ? 'flex' : 'none';
    }

    if (!initData) {
        document.getElementById('previewBanner').style.display = 'flex';
        document.getElementById('userName').textContent = 'Демо';
        updateAdminUI(true);
        allTournaments = sortTournaments(mockTournaments);
    }

    let termsAgreed = true;

    async function loadUserProfile() {
        if (!initData) return;
        try {
            const res = await fetch('/api/user/me', {
                headers: { 'X-Init-Data': initData }
            });
            if (res.ok) {
                const profile = await res.json();
                myTgId = profile.tgId;
                termsAgreed = !!profile.termsAgreed;
                if (profile.firstName || profile.username) {
                    document.getElementById('userName').textContent = profile.firstName || profile.username;
                }
                updateAdminUI(profile.isAdmin);

                if (!termsAgreed) {
                    openTermsModal();
                }
            }
        } catch (e) {
            console.error('Failed to load profile', e);
        }
    }

    function openTermsModal() {
        const modal = document.getElementById('termsAgreementModal');
        if (modal) {
            modal.classList.add('open');
            modal.classList.add('active');
        }
    }

    function closeTermsModal() {
        const modal = document.getElementById('termsAgreementModal');
        if (modal) {
            modal.classList.remove('open');
            modal.classList.remove('active');
        }
    }

    async function handleConfirmRegistration() {
        const checkbox = document.getElementById('agreeTermsCheckbox');
        if (!checkbox || !checkbox.checked) {
            showToast('Пожалуйста, подтвердите согласие с правилами', 'warning');
            return;
        }

        triggerHaptic('medium');
        const btn = document.getElementById('btnConfirmRegistration');
        if (btn) btn.disabled = true;

        if (!initData) {
            termsAgreed = true;
            closeTermsModal();
            showToast('Регистрация успешно пройдена (демо)!', 'success');
            return;
        }

        try {
            const res = await fetch('/api/user/register', {
                method: 'POST',
                headers: { 'X-Init-Data': initData }
            });
            if (res.ok) {
                const profile = await res.json();
                termsAgreed = true;
                closeTermsModal();
                showToast('Добро пожаловать в клуб!', 'success');
                loadTournaments();
            } else {
                showToast('Ошибка регистрации. Попробуйте еще раз', 'error');
                if (btn) btn.disabled = false;
            }
        } catch (e) {
            showToast('Ошибка соединения', 'error');
            if (btn) btn.disabled = false;
        }
    }

    function switchMainTab(tab) {
        triggerHaptic('medium');
        currentMainTab = tab;
        const playerView = document.getElementById('playerTournamentsView');
        const leaderboardView = document.getElementById('leaderboardPageView');
        const adminView = document.getElementById('adminPageView');

        const btnTourn = document.getElementById('tabBtnTournaments');
        const btnLead = document.getElementById('tabBtnLeaderboard');
        const btnAdm = document.getElementById('tabBtnAdmin');

        if (playerView) playerView.style.display = 'none';
        if (leaderboardView) leaderboardView.classList.remove('active');
        if (adminView) adminView.classList.remove('active');

        if (btnTourn) btnTourn.classList.remove('active');
        if (btnLead) btnLead.classList.remove('active');
        if (btnAdm) btnAdm.classList.remove('active');

        if (tab === 'admin') {
            if (adminView) adminView.classList.add('active');
            if (btnAdm) btnAdm.classList.add('active');
            updateTvRemoteUI();
            populateTvTournamentSelect();
            renderAdminTournaments();
            syncTvRemoteFromServer();
        } else if (tab === 'leaderboard') {
            if (leaderboardView) leaderboardView.classList.add('active');
            if (btnLead) btnLead.classList.add('active');
            loadActiveSeason();
            loadLeaderboard(currentLeaderboardPeriod);
        } else {
            if (playerView) playerView.style.display = 'block';
            if (btnTourn) btnTourn.classList.add('active');
        }
    }

    function switchLeaderboardPeriod(period) {
        triggerHaptic('light');
        currentLeaderboardPeriod = period;
        ['season', 'month', 'week'].forEach(p => {
            const btn = document.getElementById('btnPeriod' + p.charAt(0).toUpperCase() + p.slice(1));
            if (btn) {
                if (p === period) btn.classList.add('active');
                else btn.classList.remove('active');
            }
        });
        loadLeaderboard(period);
    }

    async function loadActiveSeason() {
        try {
            const res = await fetch('/api/public/seasons/active');
            if (res.ok && res.status !== 204) {
                currentSeason = await res.json();
                document.getElementById('seasonHeroTitle').textContent = currentSeason.name || 'Активный сезон';
                const starts = currentSeason.startsAt ? new Date(currentSeason.startsAt).toLocaleDateString('ru-RU') : '';
                document.getElementById('seasonHeroDates').textContent = starts ? `Стартовал: ${starts}` : 'Сезонный зачет';
                document.getElementById('seasonStatusBadge').style.display = 'inline-flex';
                document.getElementById('seasonStatusText').textContent = 'АКТИВЕН';
            } else {
                currentSeason = null;
                document.getElementById('seasonHeroTitle').textContent = 'Межсезонье';
                document.getElementById('seasonHeroDates').textContent = 'Сезон пока не запущен. Отображается общий зачет.';
                document.getElementById('seasonStatusBadge').style.display = 'none';
            }
        } catch (e) {
            console.warn(e);
        }
    }

    function getMockLeaderboard(period) {
        return [
            { rank: 1, firstName: 'Александр', username: 'alex_pro', tgId: 101, points: period === 'week' ? 45 : (period === 'month' ? 120 : 285), tournamentsPlayed: 8, wins: 3 },
            { rank: 2, firstName: 'Михаил', username: 'mikhail_k', tgId: 102, points: period === 'week' ? 30 : (period === 'month' ? 95 : 210), tournamentsPlayed: 6, wins: 2 },
            { rank: 3, firstName: 'Елена', username: 'elena_poker', tgId: 103, points: period === 'week' ? 25 : (period === 'month' ? 70 : 165), tournamentsPlayed: 5, wins: 1 },
            { rank: 4, firstName: 'Дмитрий', username: 'dima77', tgId: 104, points: period === 'week' ? 15 : (period === 'month' ? 50 : 110), tournamentsPlayed: 4, wins: 0 },
            { rank: 5, firstName: 'Сергей', username: 'serg_ace', tgId: 105, points: period === 'week' ? 10 : (period === 'month' ? 30 : 75), tournamentsPlayed: 3, wins: 0 }
        ];
    }

    async function loadLeaderboard(period = 'season') {
        const listContainer = document.getElementById('leaderboardList');
        const podiumContainer = document.getElementById('leaderboardPodium');
        const myRankDisplay = document.getElementById('myRankDisplay');
        const totalBadge = document.getElementById('leaderboardTotalBadge');

        let list = [];
        try {
            const res = await fetch(`/api/public/leaderboard?period=${period}`);
            if (res.ok) {
                list = await res.json();
            } else if (!initData) {
                list = getMockLeaderboard(period);
            }
        } catch (e) {
            console.warn('Error fetching leaderboard, using preview fallback if available:', e);
            if (!initData) {
                list = getMockLeaderboard(period);
            }
        }

        if (totalBadge) totalBadge.textContent = list.length;

        let myEntry = list.find(e => e.tgId == myTgId);
        if (myEntry) {
            myRankDisplay.textContent = `#${myEntry.rank} (${myEntry.points} очк.)`;
        } else {
            myRankDisplay.textContent = list.length > 0 ? 'Не в рейтинге' : '0 очков';
        }

        if (podiumContainer) {
            podiumContainer.innerHTML = '';
            if (list.length >= 1) {
                const first = list[0];
                const second = list.length >= 2 ? list[1] : null;
                const third = list.length >= 3 ? list[2] : null;

                if (second) {
                    podiumContainer.appendChild(createPodiumCard(second, 'second', '🥈', '2 МЕСТО'));
                } else {
                    podiumContainer.appendChild(createEmptyPodiumCard('2 МЕСТО', '🥈'));
                }

                podiumContainer.appendChild(createPodiumCard(first, 'first', '🥇', '1 МЕСТО'));

                if (third) {
                    podiumContainer.appendChild(createPodiumCard(third, 'third', '🥉', '3 МЕСТО'));
                } else {
                    podiumContainer.appendChild(createEmptyPodiumCard('3 МЕСТО', '🥉'));
                }
            } else {
                podiumContainer.innerHTML = `
                    <div style="grid-column: 1 / -1; text-align: center; padding: 20px; color: var(--text-secondary); font-size: 12px;">
                        В этом периоде пока нет завершенных турниров. Загрузите итоги турнира через админ-панель.
                    </div>
                `;
            }
        }

        if (listContainer) {
            listContainer.innerHTML = '';
            if (list.length === 0) {
                listContainer.innerHTML = `
                    <div class="empty-box" style="padding: 16px;">
                        <div class="empty-title">Рейтинг пуст</div>
                        <div class="empty-desc">Сыграйте турнир и загрузите таблицу с 4-м листом выбывших!</div>
                    </div>
                `;
            } else {
                list.forEach(entry => {
                    const item = document.createElement('div');
                    const isMe = entry.tgId == myTgId;
                    item.className = `leaderboard-item ${isMe ? 'is-me' : ''}`;
                    const displayName = entry.firstName || (entry.username ? '@' + entry.username : 'Игрок #' + entry.tgId);
                    const usernameSub = entry.username ? `@${entry.username}` : `ID: ${entry.tgId}`;

                    item.innerHTML = `
                        <div class="leader-rank">#${entry.rank}</div>
                        <div class="leader-info-block">
                            <span class="leader-name">${escapeHtml(displayName)} ${isMe ? '<span style="color: var(--gold-main); font-size: 10px;">(Вы)</span>' : ''}</span>
                            <span class="leader-meta">${escapeHtml(usernameSub)} • Турниров: ${entry.tournamentsPlayed} • Побед: ${entry.wins}</span>
                        </div>
                        <div class="leader-score-block">
                            <div class="leader-points">${entry.points}</div>
                            <div class="leader-points-label">очков</div>
                        </div>
                    `;
                    listContainer.appendChild(item);
                });
            }
        }
    }

    function createPodiumCard(entry, typeClass, medal, placeLabel) {
        const card = document.createElement('div');
        card.className = `podium-card ${typeClass}`;
        const displayName = entry.firstName || (entry.username ? '@' + entry.username : 'Игрок');
        const detail = entry.wins > 0 ? `${entry.wins} поб.` : `${entry.tournamentsPlayed} турн.`;
        card.innerHTML = `
            <div class="podium-medal">${medal}</div>
            <div class="podium-name" title="${escapeHtml(displayName)}">${escapeHtml(displayName)}</div>
            <div class="podium-pts">${entry.points}</div>
            <div class="podium-sub">${placeLabel} • ${detail}</div>
        `;
        return card;
    }

    function createEmptyPodiumCard(placeLabel, medal) {
        const card = document.createElement('div');
        card.className = 'podium-card';
        card.style.opacity = '0.4';
        card.innerHTML = `
            <div class="podium-medal">${medal}</div>
            <div class="podium-name">—</div>
            <div class="podium-pts">0</div>
            <div class="podium-sub">${placeLabel}</div>
        `;
        return card;
    }

    let currentParticipantsTourneyId = null;

    function downloadTournamentExcel(id) {
        const tourneyId = id || currentParticipantsTourneyId || selectedResultsTourneyId;
        if (!tourneyId) {
            showToast('Выберите турнир для выгрузки Excel', 'warning');
            return;
        }
        triggerHaptic('medium');
        const url = `/api/public/tournaments/${tourneyId}/export-excel`;
        if (tg && typeof tg.openLink === 'function') {
            tg.openLink(window.location.origin + url);
        } else {
            const a = document.createElement('a');
            a.href = url;
            a.download = `tournament-${tourneyId}.xlsx`;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
        }
        showToast('Таблица формируется (4 листа с игроками)', 'success');
    }

    function openStartSeasonModal() {
        triggerHaptic('medium');
        const modal = document.getElementById('seasonModal');
        if (modal) {
            modal.classList.add('open');
            modal.classList.add('active');
        }
    }
    function closeSeasonModal() {
        const modal = document.getElementById('seasonModal');
        if (modal) {
            modal.classList.remove('open');
            modal.classList.remove('active');
        }
    }
    function closeSeasonModalOnBackdrop(e) {
        if (e.target.id === 'seasonModal') closeSeasonModal();
    }
    async function handleStartSeasonSubmit(e) {
        e.preventDefault();
        const name = document.getElementById('seasonNameInput').value.trim();
        if (!name) return;

        if (!initData) {
            closeSeasonModal();
            showToast(`Сезон «${name}» успешно начат (демо)!`, 'success');
            currentSeason = { name, startsAt: new Date().toISOString(), active: true };
            document.getElementById('seasonHeroTitle').textContent = name;
            document.getElementById('seasonHeroDates').textContent = 'Стартовал: только что';
            document.getElementById('seasonStatusBadge').style.display = 'inline-flex';
            document.getElementById('seasonStatusText').textContent = 'АКТИВЕН';
            return;
        }

        try {
            const res = await fetch('/api/admin/seasons/start', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Init-Data': initData
                },
                body: JSON.stringify({ name })
            });
            if (res.ok) {
                closeSeasonModal();
                showToast(`Сезон «${name}» успешно начат!`, 'success');
                loadActiveSeason();
                loadLeaderboard(currentLeaderboardPeriod);
            } else {
                const err = await res.json().catch(() => ({}));
                showToast(err.message || 'Ошибка запуска сезона', 'error');
            }
        } catch (err) {
            showToast('Сбой соединения: ' + err.message, 'error');
        }
    }

    function handleEndSeason() {
        triggerHaptic('warning');
        const doEnd = async () => {
            if (!initData) {
                showToast('Текущий сезон завершен (демо)', 'success');
                currentSeason = null;
                document.getElementById('seasonHeroTitle').textContent = 'Межсезонье';
                document.getElementById('seasonHeroDates').textContent = 'Сезон завершен.';
                document.getElementById('seasonStatusBadge').style.display = 'none';
                return;
            }

            try {
                const res = await fetch('/api/admin/seasons/end', {
                    method: 'POST',
                    headers: { 'X-Init-Data': initData }
                });
                if (res.ok) {
                    showToast('Текущий сезон завершен', 'success');
                    loadActiveSeason();
                    loadLeaderboard(currentLeaderboardPeriod);
                } else {
                    const err = await res.json().catch(() => ({}));
                    showToast(err.message || 'Ошибка завершения сезона', 'error');
                }
            } catch (err) {
                showToast('Сбой соединения: ' + err.message, 'error');
            }
        };

        if (tg && typeof tg.showConfirm === 'function') {
            tg.showConfirm('Вы уверены, что хотите завершить текущий сезон? Все результаты будут зафиксированы.', (confirmed) => {
                if (confirmed) doEnd();
            });
        } else {
            if (confirm('Вы уверены, что хотите завершить текущий сезон? Все результаты будут зафиксированы.')) {
                doEnd();
            }
        }
    }

    function openResultsModal(tourneyId, title) {
        triggerHaptic('medium');
        selectedResultsTourneyId = tourneyId;
        const heading = document.getElementById('resultsModalTitle');
        if (heading) heading.textContent = `Итоги: ${title}`;
        const modal = document.getElementById('resultsModal');
        if (modal) {
            modal.classList.add('open');
            modal.classList.add('active');
        }
    }
    function closeResultsModal() {
        const modal = document.getElementById('resultsModal');
        if (modal) {
            modal.classList.remove('open');
            modal.classList.remove('active');
        }
        selectedResultsTourneyId = null;
    }
    function closeResultsModalOnBackdrop(e) {
        if (e.target.id === 'resultsModal') closeResultsModal();
    }
    async function submitResultsFromUrl() {
        if (!selectedResultsTourneyId) return;

        if (!initData) {
            closeResultsModal();
            showToast('Демо: итоги рассчитаны по таблице, рейтинг обновлен!', 'success');
            loadLeaderboard(currentLeaderboardPeriod);
            return;
        }

        const btn = document.getElementById('btnResultsFromUrl');
        btn.textContent = 'Обработка 4-го листа...';
        btn.disabled = true;
        try {
            const res = await fetch(`/api/admin/tournaments/${selectedResultsTourneyId}/results/from-url`, {
                method: 'POST',
                headers: { 'X-Init-Data': initData }
            });
            const data = await res.json();
            if (res.ok && data.success) {
                closeResultsModal();
                showToast(`Готово! Обработано ${data.processedPlayers} игроков, рейтинг обновлен!`, 'success');
                loadTournaments();
                loadLeaderboard(currentLeaderboardPeriod);
            } else {
                showToast(data.message || 'Ошибка обработки таблицы', 'error');
            }
        } catch (err) {
            showToast('Ошибка: ' + err.message, 'error');
        } finally {
            btn.textContent = 'Рассчитать итоги по ссылке';
            btn.disabled = false;
        }
    }
    async function submitResultsFile() {
        if (!selectedResultsTourneyId) return;
        const fileInput = document.getElementById('resultsFileInput');
        if (!fileInput.files || fileInput.files.length === 0) {
            showToast('Выберите файл .xlsx', 'error');
            return;
        }

        if (!initData) {
            closeResultsModal();
            showToast('Демо: файл .xlsx загружен, очки начислены!', 'success');
            loadLeaderboard(currentLeaderboardPeriod);
            return;
        }

        const btn = document.getElementById('btnResultsUploadFile');
        btn.textContent = 'Загрузка...';
        btn.disabled = true;
        const formData = new FormData();
        formData.append('file', fileInput.files[0]);
        try {
            const res = await fetch(`/api/admin/tournaments/${selectedResultsTourneyId}/results/upload`, {
                method: 'POST',
                headers: { 'X-Init-Data': initData },
                body: formData
            });
            const data = await res.json();
            if (res.ok && data.success) {
                closeResultsModal();
                showToast(`Успешно! Начислены очки для ${data.processedPlayers} игроков!`, 'success');
                loadTournaments();
                loadLeaderboard(currentLeaderboardPeriod);
            } else {
                showToast(data.message || 'Ошибка загрузки файла', 'error');
            }
        } catch (err) {
            showToast('Ошибка: ' + err.message, 'error');
        } finally {
            btn.textContent = 'Загрузить файл и начислить очки';
            btn.disabled = false;
        }
    }

    async function loadTournaments() {
        const refreshBtn = document.getElementById('refreshBtn');
        refreshBtn.classList.add('spinning');

        if (!initData) {
            setTimeout(() => {
                refreshBtn.classList.remove('spinning');
                allTournaments = sortTournaments(mockTournaments);
                renderTournaments(allTournaments);
                updateStats(allTournaments);
                renderAdminTournaments();
                populateTvTournamentSelect();
            }, 300);
            return;
        }

        try {
            const res = await fetch('/api/tournaments', {
                headers: { 'X-Init-Data': initData }
            });

            refreshBtn.classList.remove('spinning');

            if (!res.ok) {
                if (res.status === 401) {
                    renderAuthRequiredState();
                    return;
                }
                const errText = await res.text();
                renderErrorState(errText || 'Не удалось загрузить данные');
                return;
            }

            const data = await res.json();
            allTournaments = sortTournaments(Array.isArray(data) ? data : []);

            if (allTournaments.length === 0) {
                renderEmptyState();
            } else {
                renderTournaments(allTournaments);
            }
            updateStats(allTournaments);
            renderAdminTournaments();
            populateTvTournamentSelect();

        } catch (error) {
            refreshBtn.classList.remove('spinning');
            renderErrorState('Ошибка сети');
        }
    }

    function updateStats(tournaments) {
        document.getElementById('tournamentsCount').textContent = tournaments.length;
        document.getElementById('activeBadge').textContent = tournaments.length;
        const regCount = tournaments.filter(t => t.registered).length;
        document.getElementById('userRegCount').textContent = regCount;
    }

    function formatStartsAt(isoString) {
        if (!isoString) return null;
        try {
            const d = new Date(isoString);
            return d.toLocaleDateString('ru-RU', {
                day: 'numeric',
                month: 'short',
                hour: '2-digit',
                minute: '2-digit'
            });
        } catch (e) {
            return null;
        }
    }

    function formatForDateTimeInput(isoString) {
        if (!isoString) return '';
        try {
            const d = new Date(isoString);
            const pad = n => String(n).padStart(2, '0');
            return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
        } catch (e) {
            return '';
        }
    }

    function formatTime(seconds) {
        const m = Math.floor(seconds / 60);
        const s = seconds % 60;
        return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
    }

    function renderTournaments(tournaments) {
        const list = document.getElementById('list');
        list.innerHTML = '';

        if (!tournaments || tournaments.length === 0) {
            renderEmptyState('Турниры не найдены');
            return;
        }

        const sorted = sortTournaments(tournaments);

        sorted.forEach(t => {
            const isReg = !!t.registered;
            const card = document.createElement('div');
            card.className = 'tournament-card';
            card.id = `tournament-${t.id}`;

            const formattedDate = formatStartsAt(t.startsAt);

            card.innerHTML = `
                <div class="card-top">
                    <div class="card-top-left">
                        <span class="card-id-pill">#${t.id}</span>
                        ${t.seasonName ? `<span class="card-id-pill" style="border-color: rgba(223, 177, 91, 0.3); color: var(--gold-light);">${escapeHtml(t.seasonName)}</span>` : ''}
                    </div>
                    ${t.finished ? `
                        <span class="card-status-pill" style="color: #6ee7b7; background: rgba(16, 185, 129, 0.15); border-color: rgba(16, 185, 129, 0.4);">
                            Завершен
                        </span>
                    ` : `
                        <span class="card-status-pill">
                            <span class="status-dot-pulse"></span>
                            Открыт
                        </span>
                    `}
                </div>

                <h3 class="card-title">${escapeHtml(t.title)}</h3>

                ${(formattedDate || t.maxPlayers || t.participantsCount) ? `
                    <div class="card-details">
                        ${formattedDate ? `
                            <span class="detail-item">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                    <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
                                    <line x1="16" y1="2" x2="16" y2="6"></line>
                                    <line x1="8" y1="2" x2="8" y2="6"></line>
                                    <line x1="3" y1="10" x2="21" y2="10"></line>
                                </svg>
                                ${escapeHtml(formattedDate)}
                            </span>
                        ` : ''}
                        ${t.maxPlayers ? `
                            <span class="detail-item">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                                    <circle cx="9" cy="7" r="4"></circle>
                                    <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                                    <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                                </svg>
                                ${t.participantsCount || 0} / ${t.maxPlayers} мест
                            </span>
                        ` : (t.participantsCount ? `
                            <span class="detail-item">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                                    <circle cx="9" cy="7" r="4"></circle>
                                </svg>
                                ${t.participantsCount} уч.
                            </span>
                        ` : '')}
                    </div>
                ` : ''}

                <div class="card-actions-row">
                    <button type="button" class="btn-card-secondary" onclick="openParticipantsModal(${t.id})">
                        <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                            <circle cx="9" cy="7" r="4"></circle>
                            <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                            <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                        </svg>
                        Участники (${t.participantsCount || 0})
                    </button>
                    ${!t.finished ? `
                    <button id="reg-btn-${t.id}"
                            class="btn-action ${isReg ? 'state-registered' : 'state-unregistered'}"
                            onclick="toggleRegistration(${t.id}, ${isReg})"
                            onmouseenter="handleBtnHover(${t.id}, ${isReg}, true)"
                            onmouseleave="handleBtnHover(${t.id}, ${isReg}, false)">
                        <span id="reg-text-${t.id}">
                            ${isReg ? 'Вы записаны' : 'Записаться'}
                        </span>
                    </button>
                    ` : ''}
                </div>
            `;
            list.appendChild(card);
        });
    }

    function handleBtnHover(id, isReg, isHover) {
        if (!isReg) return;
        const textSpan = document.getElementById(`reg-text-${id}`);
        if (textSpan) {
            textSpan.textContent = isHover ? 'Отменить запись' : 'Вы записаны';
        }
    }

    async function toggleRegistration(id, currentlyRegistered) {
        const target = allTournaments.find(x => x.id === id);
        if (target && target.finished) {
            showToast('Турнир уже завершен', 'warning');
            return;
        }
        triggerHaptic('heavy');
        const btn = document.getElementById(`reg-btn-${id}`);
        if (!btn || btn.classList.contains('loading')) return;

        const textSpan = document.getElementById(`reg-text-${id}`);
        btn.classList.add('loading');
        textSpan.textContent = 'Обработка...';

        if (!initData) {
            setTimeout(() => {
                btn.classList.remove('loading');
                if (target) {
                    target.registered = !currentlyRegistered;
                    if (target.registered) {
                        target.participantsCount = (target.participantsCount || 0) + 1;
                    } else if (target.participantsCount > 0) {
                        target.participantsCount -= 1;
                    }
                }
                renderTournaments(allTournaments);
                updateStats(allTournaments);
                showToast(!currentlyRegistered ? 'Запись подтверждена' : 'Запись отменена', !currentlyRegistered ? 'success' : 'info');
            }, 350);
            return;
        }

        const endpoint = currentlyRegistered
            ? `/api/tournaments/${id}/unregister`
            : `/api/tournaments/${id}/register`;

        try {
            const res = await fetch(endpoint, {
                method: 'POST',
                headers: { 'X-Init-Data': initData }
            });

            btn.classList.remove('loading');
            const reply = await res.text();

            if (res.ok) {
                if (target) {
                    target.registered = !currentlyRegistered;
                }
                renderTournaments(allTournaments);
                updateStats(allTournaments);
                showToast(reply || (!currentlyRegistered ? 'Вы записаны' : 'Запись отменена'), 'success');
            } else {
                textSpan.textContent = currentlyRegistered ? 'Вы записаны' : 'Записаться';
                showToast(reply || 'Ошибка запроса', 'error');
            }
        } catch (error) {
            btn.classList.remove('loading');
            textSpan.textContent = currentlyRegistered ? 'Вы записаны' : 'Записаться';
            showToast('Ошибка сети', 'error');
        }
    }

    function renderAdminTournaments() {
        const container = document.getElementById('adminTournamentsList');
        if (!container) return;
        container.innerHTML = '';

        if (allTournaments.length === 0) {
            container.innerHTML = `
                <div class="empty-box" style="padding: 24px;">
                    <div class="empty-title">Нет турниров</div>
                    <div class="empty-desc">Создайте первый турнир кнопкой выше</div>
                </div>
            `;
            return;
        }

        const sorted = sortTournaments(allTournaments);

        sorted.forEach(t => {
            const card = document.createElement('div');
            const isActiveOnTv = tvState.tournamentId == t.id;
            card.className = `admin-tourney-card ${isActiveOnTv ? 'active-on-tv' : ''}`;

            const formattedDate = formatStartsAt(t.startsAt);

            card.innerHTML = `
                <div class="admin-tourney-top">
                    <div style="display: flex; align-items: center; gap: 6px;">
                        <span class="card-id-pill">ID #${t.id}</span>
                        ${t.finished ? `<span class="card-id-pill" style="border-color: rgba(16, 185, 129, 0.4); background: rgba(16, 185, 129, 0.15); color: #6ee7b7;">Завершен</span>` : ''}
                        ${t.seasonName ? `<span class="card-id-pill" style="border-color: rgba(223, 177, 91, 0.3); color: var(--gold-light);">${escapeHtml(t.seasonName)}</span>` : ''}
                    </div>
                    ${isActiveOnTv ? `<span class="card-id-pill" style="border-color: var(--gold-main); background: rgba(223, 177, 91, 0.2);">В эфире на ТВ</span>` : ''}
                </div>

                <div class="admin-tourney-title">${escapeHtml(t.title)}</div>

                <div class="card-details" style="margin-bottom: 0;">
                    ${formattedDate ? `<span class="detail-item">Дата: ${escapeHtml(formattedDate)}</span>` : ''}
                    <span class="detail-item">Мест: ${t.participantsCount || 0} / ${t.maxPlayers || 'без лимита'}</span>
                </div>

                <div class="admin-actions-bar">
                    <button type="button" class="btn-admin-act tv-btn" onclick="setTvTournamentDirect(${t.id})">
                        На ТВ
                    </button>
                    <button type="button" class="btn-admin-act" onclick="downloadTournamentExcel(${t.id})" title="Выгрузить Excel турнира с 4 листами и игроками">
                        Выгрузить Excel
                    </button>
                    <button type="button" class="btn-admin-act" style="background: rgba(16, 185, 129, 0.12); border-color: rgba(16, 185, 129, 0.35); color: #6ee7b7;" onclick="openResultsModal(${t.id}, '${escapeHtml(t.title)}')">
                        ${t.finished ? 'Итоги ✓' : 'Итоги / Загрузка'}
                    </button>
                    <button type="button" class="btn-admin-act" onclick="openParticipantsModal(${t.id})">
                        Участники (${t.participantsCount || 0})
                    </button>
                    <button type="button" class="btn-admin-act" onclick="openEditModal(${t.id})">
                        Изменить
                    </button>
                    <button type="button" class="btn-admin-act danger-btn" onclick="handleDeleteTournament(${t.id})">
                        Удалить
                    </button>
                </div>
            `;
            container.appendChild(card);
        });
    }

    function populateTvTournamentSelect() {
        const select = document.getElementById('tvSelectTournament');
        if (!select) return;
        select.innerHTML = '';

        if (allTournaments.length === 0) {
            const opt = document.createElement('option');
            opt.value = '';
            opt.textContent = 'Нет доступных турниров';
            select.appendChild(opt);
            return;
        }

        allTournaments.forEach(t => {
            const opt = document.createElement('option');
            opt.value = t.id;
            opt.textContent = `#${t.id} — ${t.title}`;
            if (tvState.tournamentId == t.id) opt.selected = true;
            select.appendChild(opt);
        });
    }

    async function syncTvRemoteFromServer() {
        try {
            const res = await fetch('/api/public/tv/state');
            if (res.ok) {
                const s = await res.json();
                if (s) {
                    const isRunning = s.running !== undefined ? !!s.running : !!s.isRunning;
                    const isBreak = s.break !== undefined ? !!s.break : !!s.isBreak;
                    tvState = {
                        tournamentId: s.tournamentId,
                        tournamentTitle: s.tournamentTitle,
                        excelUrl: s.excelUrl,
                        running: isRunning,
                        isRunning: isRunning,
                        currentLevel: s.currentLevel || 1,
                        secondsLeft: s.secondsLeft !== undefined ? s.secondsLeft : 15 * 60,
                        break: isBreak,
                        isBreak: isBreak,
                        smallBlind: s.smallBlind,
                        bigBlind: s.bigBlind,
                        ante: s.ante
                    };
                    updateTvRemoteUI();
                    populateTvTournamentSelect();
                    renderAdminTournaments();
                }
            }
        } catch (e) {}
    }

    function updateBlindsCalcPreview(bbVal) {
        const preview = document.getElementById('tvBlindsCalcPreview');
        if (!preview) return;
        const bb = parseInt(bbVal, 10);
        if (bb && bb > 0) {
            const sb = Math.max(1, Math.floor(bb / 2));
            const ante = bb;
            preview.textContent = `МБ: ${sb.toLocaleString('ru-RU')} / ББ: ${bb.toLocaleString('ru-RU')} (${ante.toLocaleString('ru-RU')})`;
        } else {
            preview.textContent = 'МБ: — / ББ: —';
        }
    }

    async function handleApplyBlinds() {
        const input = document.getElementById('tvInputBigBlind');
        if (!input) return;
        const bb = parseInt(input.value, 10);
        if (!bb || bb <= 0) {
            showToast('Введите размер большого блайнда (BB)', 'warning');
            return;
        }
        const sb = Math.max(1, Math.floor(bb / 2));
        const ante = bb;
        await sendTvAction('SET_BLINDS', { bigBlind: bb, smallBlind: sb, ante: ante });
        showToast(`Блайнды ${sb.toLocaleString('ru-RU')} / ${bb.toLocaleString('ru-RU')} выведены на ТВ`, 'success');
    }

    function updateTvRemoteUI() {
        const badge = document.getElementById('tvRemoteStatusBadge');
        const badgeText = document.getElementById('tvRemoteStatusText');
        const timerElem = document.getElementById('tvRemoteTimerDisplay');
        const btnPlay = document.getElementById('tvRemoteBtnPlay');
        const levelNum = document.getElementById('tvRemoteLevelNum');
        const blindsText = document.getElementById('tvRemoteBlinds');

        if (!badge || !badgeText || !timerElem || !btnPlay) return;

        timerElem.textContent = formatTime(tvState.secondsLeft);

        badge.className = 'tv-remote-status';
        const isBreak = !!(tvState.break ?? tvState.isBreak);
        const isRunning = !!(tvState.running ?? tvState.isRunning);

        if (isBreak) {
            badge.classList.add('break');
            badgeText.textContent = 'ПЕРЕРЫВ';
            if (blindsText) blindsText.textContent = 'Перерыв';
            if (levelNum) levelNum.textContent = 'Перерыв';
        } else if (isRunning) {
            badge.classList.add('running');
            badgeText.textContent = 'В ЭФИРЕ (РАЗЫГРОВКА)';
        } else {
            badge.classList.add('paused');
            badgeText.textContent = 'ПАУЗА';
        }

        if (!isBreak) {
            const lvl = tvState.currentLevel || 1;
            if (levelNum) levelNum.textContent = lvl;
            const sb = (tvState.smallBlind !== undefined && tvState.smallBlind !== null) ? tvState.smallBlind : 100;
            const bb = (tvState.bigBlind !== undefined && tvState.bigBlind !== null) ? tvState.bigBlind : 200;
            const ante = (tvState.ante !== undefined && tvState.ante !== null) ? tvState.ante : 200;
            if (blindsText) {
                blindsText.textContent = `${sb.toLocaleString('ru-RU')} / ${bb.toLocaleString('ru-RU')} (${ante.toLocaleString('ru-RU')})`;
            }
            const inputBb = document.getElementById('tvInputBigBlind');
            if (inputBb && document.activeElement !== inputBb && !inputBb.value) {
                inputBb.value = bb;
                updateBlindsCalcPreview(bb);
            }
        }

        if (isRunning) {
            btnPlay.classList.add('is-pause');
            btnPlay.textContent = 'Поставить на паузу (ТВ)';
        } else {
            btnPlay.classList.remove('is-pause');
            btnPlay.textContent = 'Запустить таймер на ТВ';
        }
    }

    async function sendTvAction(action, extra = {}) {
        triggerHaptic('medium');
        if (!initData) {

            if (action === 'START') {
                tvState.running = true;
                tvState.isRunning = true;
            } else if (action === 'PAUSE') {
                tvState.running = false;
                tvState.isRunning = false;
            } else if (action === 'NEXT_LEVEL') {
                tvState.currentLevel = (tvState.currentLevel || 1) + 1;
                const b = BLINDS_TABLE[(tvState.currentLevel || 1) - 1] || BLINDS_TABLE[0];
                const mins = b && b.minutes ? b.minutes : 20;
                tvState.secondsLeft = mins * 60;
                tvState.break = !!(b && b.isBreak);
                tvState.isBreak = !!(b && b.isBreak);
                if (b && !b.isBreak) {
                    tvState.smallBlind = b.sb;
                    tvState.bigBlind = b.bb;
                    tvState.ante = b.ante;
                }
            } else if (action === 'PREV_LEVEL') {
                if ((tvState.currentLevel || 1) > 1) {
                    tvState.currentLevel--;
                }
                const b = BLINDS_TABLE[(tvState.currentLevel || 1) - 1] || BLINDS_TABLE[0];
                const mins = b && b.minutes ? b.minutes : 20;
                tvState.secondsLeft = mins * 60;
                tvState.break = !!(b && b.isBreak);
                tvState.isBreak = !!(b && b.isBreak);
                if (b && !b.isBreak) {
                    tvState.smallBlind = b.sb;
                    tvState.bigBlind = b.bb;
                    tvState.ante = b.ante;
                }
            } else if (action === 'TOGGLE_BREAK') {
                const nextBreak = !(tvState.break ?? tvState.isBreak);
                tvState.break = nextBreak;
                tvState.isBreak = nextBreak;
                tvState.secondsLeft = nextBreak ? 15 * 60 : 20 * 60;
            } else if (action === 'ADJUST_TIME') {
                const delta = extra.deltaSeconds || 0;
                tvState.secondsLeft = Math.max(0, tvState.secondsLeft + delta);
            } else if (action === 'SYNC') {
                tvState.secondsLeft = extra.secondsLeft !== undefined ? extra.secondsLeft : 20 * 60;
            } else if (action === 'SET_BLINDS') {
                tvState.bigBlind = extra.bigBlind;
                tvState.smallBlind = extra.smallBlind;
                tvState.ante = extra.ante;
            } else if (action === 'SET_TOURNAMENT') {
                tvState.tournamentId = extra.tournamentId;
                const found = allTournaments.find(x => x.id == extra.tournamentId);
                if (found) tvState.tournamentTitle = found.title;
            }
            updateTvRemoteUI();
            renderAdminTournaments();
            showToast(`Команда выполнена: ${action}`);
            return;
        }

        try {
            const res = await fetch('/api/admin/tv/action', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Init-Data': initData
                },
                body: JSON.stringify({ action, ...extra })
            });

            if (res.ok) {
                const s = await res.json();
                const isRunning = s.running !== undefined ? !!s.running : !!s.isRunning;
                const isBreak = s.break !== undefined ? !!s.break : !!s.isBreak;
                tvState = {
                    tournamentId: s.tournamentId,
                    tournamentTitle: s.tournamentTitle,
                    excelUrl: s.excelUrl,
                    running: isRunning,
                    isRunning: isRunning,
                    currentLevel: s.currentLevel || 1,
                    secondsLeft: s.secondsLeft !== undefined ? s.secondsLeft : 20 * 60,
                    break: isBreak,
                    isBreak: isBreak,
                    smallBlind: s.smallBlind,
                    bigBlind: s.bigBlind,
                    ante: s.ante
                };
                updateTvRemoteUI();
                renderAdminTournaments();
                populateTvTournamentSelect();
                showToast('Команда на ТВ отправлена', 'success');
            } else {
                const errText = await res.text();
                showToast(errText || 'Ошибка отправки команды на ТВ', 'error');
            }
        } catch (e) {
            showToast('Ошибка сети', 'error');
        }
    }

    function tvRemoteTogglePlay() {
        const isRunning = !!(tvState.running ?? tvState.isRunning);
        if (isRunning) {
            sendTvAction('PAUSE');
        } else {
            sendTvAction('START');
        }
    }

    function tvRemoteAction(action, payload = {}) {
        sendTvAction(action, payload);
    }

    function applySelectedTournamentToTv() {
        const select = document.getElementById('tvSelectTournament');
        if (!select || !select.value) {
            showToast('Выберите турнир из списка', 'info');
            return;
        }
        const id = parseInt(select.value, 10);
        if (id) {
            sendTvAction('SET_TOURNAMENT', { tournamentId: id });
        }
    }

    function setTvTournamentDirect(id) {
        if (!id) return;
        sendTvAction('SET_TOURNAMENT', { tournamentId: Number(id) });
    }

    function openTvTab() {
        triggerHaptic('medium');
        const id = tvState.tournamentId || (allTournaments[0] ? allTournaments[0].id : '');
        const path = id ? `/tv.html?tournamentId=${id}` : '/tv.html';
        const fullUrl = new URL(path, window.location.href).href;
        if (tg && typeof tg.openLink === 'function') {
            try {
                tg.openLink(fullUrl);
                return;
            } catch (e) {}
        }
        window.open(fullUrl, '_blank');
    }

    async function copyTvLink() {
        triggerHaptic('medium');
        const id = tvState.tournamentId || (allTournaments[0] ? allTournaments[0].id : '');
        const path = id ? `/tv.html?tournamentId=${id}` : '/tv.html';
        const fullUrl = new URL(path, window.location.href).href;

        let copied = false;
        if (navigator.clipboard && window.isSecureContext) {
            try {
                await navigator.clipboard.writeText(fullUrl);
                copied = true;
            } catch (e) {}
        }

        if (!copied) {
            try {
                const textArea = document.createElement('textarea');
                textArea.value = fullUrl;
                textArea.style.position = 'fixed';
                textArea.style.left = '-9999px';
                textArea.style.top = '-9999px';
                document.body.appendChild(textArea);
                textArea.focus();
                textArea.select();
                copied = document.execCommand('copy');
                document.body.removeChild(textArea);
            } catch (e) {}
        }

        if (copied) {
            showToast('Ссылка на ТВ табло скопирована', 'success');
        } else {
            showToast('Ссылка: ' + fullUrl, 'info');
        }
    }

    async function openParticipantsModal(tournamentId) {
        triggerHaptic('medium');
        currentParticipantsTourneyId = tournamentId;
        const target = allTournaments.find(x => x.id == tournamentId);
        const title = target ? target.title : '#' + tournamentId;
        document.getElementById('participantsHeading').textContent = `Участники: ${title}`;
        const container = document.getElementById('participantsList');
        container.innerHTML = '<div style="text-align: center; color: var(--text-muted); padding: 20px;">Загрузка...</div>';
        document.getElementById('participantsModal').classList.add('open');

        if (!initData) {
            setTimeout(() => {
                container.innerHTML = `
                    <div class="participant-item">
                        <div class="participant-info">
                            <span class="participant-name">Алексей Смирнов</span>
                            <span class="participant-user">@alex_poker</span>
                        </div>
                        <span class="participant-date">Вчера, 19:40</span>
                    </div>
                    <div class="participant-item">
                        <div class="participant-info">
                            <span class="participant-name">Михаил Петров</span>
                            <span class="participant-user">@petrov_m</span>
                        </div>
                        <span class="participant-date">Сегодня, 11:15</span>
                    </div>
                `;
            }, 200);
            return;
        }

        try {
            const res = await fetch(`/api/tournaments/${tournamentId}/participants`, {
                headers: { 'X-Init-Data': initData }
            });
            if (res.ok) {
                const list = await res.json();
                if (!list || list.length === 0) {
                    container.innerHTML = '<div style="text-align: center; color: var(--text-muted); padding: 24px;">На этот турнир пока никто не записался</div>';
                    return;
                }

                container.innerHTML = '';
                list.forEach((p, idx) => {
                    const row = document.createElement('div');
                    row.className = 'participant-item';
                    const name = [p.firstName, p.lastName].filter(Boolean).join(' ') || 'Игрок #' + (idx + 1);
                    const username = p.username ? `@${p.username}` : `TG: ${p.tgId}`;
                    const dateStr = p.registeredAt ? new Date(p.registeredAt).toLocaleDateString('ru-RU', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' }) : '';

                    row.innerHTML = `
                        <div class="participant-info">
                            <span class="participant-name">${escapeHtml(name)}</span>
                            <span class="participant-user">${escapeHtml(username)}</span>
                        </div>
                        <span class="participant-date">${escapeHtml(dateStr)}</span>
                    `;
                    container.appendChild(row);
                });
            } else {
                const err = await res.text();
                container.innerHTML = `<div style="text-align: center; color: var(--crimson-text); padding: 20px;">${escapeHtml(err || 'Не удалось загрузить список')}</div>`;
            }
        } catch (e) {
            container.innerHTML = '<div style="text-align: center; color: var(--crimson-text); padding: 20px;">Ошибка связи</div>';
        }
    }

    function closeParticipantsModal() {
        document.getElementById('participantsModal').classList.remove('open');
    }

    function closeParticipantsModalOnBackdrop(e) {
        if (e.target === document.getElementById('participantsModal')) {
            closeParticipantsModal();
        }
    }

    function askConfirmation(message, onConfirm) {
        if (tg && typeof tg.showConfirm === 'function') {
            tg.showConfirm(message, (confirmed) => {
                if (confirmed) onConfirm();
            });
        } else {
            if (window.confirm(message)) {
                onConfirm();
            }
        }
    }

    function handleDeleteTournament(id) {
        triggerHaptic('warning');
        askConfirmation(`Удалить турнир #${id}? Все записи на него также будут удалены.`, () => {
            performDeleteTournament(id);
        });
    }

    async function performDeleteTournament(id) {
        if (!initData) {
            allTournaments = allTournaments.filter(x => x.id != id);
            renderTournaments(allTournaments);
            updateStats(allTournaments);
            renderAdminTournaments();
            populateTvTournamentSelect();
            showToast('Турнир удален', 'success');
            return;
        }

        try {
            const res = await fetch(`/api/tournaments/${id}`, {
                method: 'DELETE',
                headers: { 'X-Init-Data': initData }
            });

            if (res.ok) {
                showToast('Турнир успешно удален', 'success');
                loadTournaments();
            } else {
                const err = await res.text();
                showToast(err || 'Не удалось удалить турнир', 'error');
            }
        } catch (e) {
            showToast('Ошибка сети', 'error');
        }
    }

    function openCreateModal() {
        triggerHaptic('medium');
        document.getElementById('editTourneyId').value = '';
        document.getElementById('modalHeading').textContent = 'Новый турнир';
        document.getElementById('btnSubmitTournament').textContent = 'Создать турнир';
        document.getElementById('tourneyTitle').value = '';
        document.getElementById('tourneyExcelUrl').value = '';
        document.getElementById('tourneyStartsAt').value = '';
        document.getElementById('tourneyMaxPlayers').value = '';
        document.getElementById('tournamentModal').classList.add('open');
    }

    function openEditModal(id) {
        triggerHaptic('medium');
        const t = allTournaments.find(x => x.id == id);
        if (!t) return;

        document.getElementById('editTourneyId').value = id;
        document.getElementById('modalHeading').textContent = `Турнир #${id}`;
        document.getElementById('btnSubmitTournament').textContent = 'Сохранить изменения';
        document.getElementById('tourneyTitle').value = t.title || '';
        document.getElementById('tourneyExcelUrl').value = t.excelUrl || '';
        document.getElementById('tourneyStartsAt').value = formatForDateTimeInput(t.startsAt);
        document.getElementById('tourneyMaxPlayers').value = t.maxPlayers || '';
        document.getElementById('tournamentModal').classList.add('open');
    }

    function closeTournamentModal() {
        document.getElementById('tournamentModal').classList.remove('open');
    }

    function closeModalOnBackdrop(e) {
        if (e.target === document.getElementById('tournamentModal')) {
            closeTournamentModal();
        }
    }

    async function handleSaveTournament(event) {
        event.preventDefault();
        triggerHaptic('heavy');

        const editId = document.getElementById('editTourneyId').value;
        const isEditing = !!editId;

        const title = document.getElementById('tourneyTitle').value.trim();
        const excelUrl = document.getElementById('tourneyExcelUrl').value.trim();
        const startsAtVal = document.getElementById('tourneyStartsAt').value;
        const maxPlayersVal = document.getElementById('tourneyMaxPlayers').value;
        const submitBtn = document.getElementById('btnSubmitTournament');

        let startsAt = null;
        if (startsAtVal) {
            try { startsAt = new Date(startsAtVal).toISOString(); } catch (e) {}
        }
        const maxPlayers = maxPlayersVal ? parseInt(maxPlayersVal, 10) : null;

        submitBtn.disabled = true;
        submitBtn.textContent = 'Сохранение...';

        if (!initData) {
            setTimeout(() => {
                submitBtn.disabled = false;
                if (isEditing) {
                    const target = allTournaments.find(x => x.id == editId);
                    if (target) {
                        target.title = title;
                        target.excelUrl = excelUrl;
                        target.startsAt = startsAt;
                        target.maxPlayers = maxPlayers;
                    }
                    showToast('Турнир обновлен', 'success');
                    allTournaments.push({
                        id: newId,
                        title,
                        excelUrl,
                        startsAt,
                        maxPlayers,
                        participantsCount: 0,
                        registered: false,
                        finished: false
                    });
                    showToast('Турнир создан', 'success');
                }
                allTournaments = sortTournaments(allTournaments);
                closeTournamentModal();
                event.target.reset();
                renderTournaments(allTournaments);
                updateStats(allTournaments);
                renderAdminTournaments();
                populateTvTournamentSelect();
            }, 300);
            return;
        }

        const endpoint = isEditing ? `/api/tournaments/${editId}` : '/api/tournaments';
        const method = isEditing ? 'PUT' : 'POST';

        try {
            const res = await fetch(endpoint, {
                method,
                headers: {
                    'Content-Type': 'application/json',
                    'X-Init-Data': initData
                },
                body: JSON.stringify({ title, excelUrl, startsAt, maxPlayers })
            });

            submitBtn.disabled = false;

            if (res.ok) {
                closeTournamentModal();
                event.target.reset();
                showToast(isEditing ? 'Турнир успешно изменен' : 'Турнир создан', 'success');
                loadTournaments();
            } else {
                const err = await res.text();
                showToast(err || 'Ошибка сохранения', 'error');
            }
        } catch (e) {
            submitBtn.disabled = false;
            showToast('Ошибка сети', 'error');
        }
    }

    function handleRefresh() {
        triggerHaptic('medium');
        loadTournaments();
        if (isAdmin) syncTvRemoteFromServer();
    }

    function filterTournaments() {
        const query = document.getElementById('searchInput').value.toLowerCase().trim();
        if (!query) {
            renderTournaments(allTournaments);
            return;
        }
        const filtered = allTournaments.filter(t =>
            (t.title && t.title.toLowerCase().includes(query)) ||
            (t.id && t.id.toString().includes(query))
        );
        renderTournaments(filtered);
    }

    function renderEmptyState(msg = 'Нет запланированных турниров') {
        const list = document.getElementById('list');
        list.innerHTML = `
            <div class="empty-box">
                <div class="empty-title">Список пуст</div>
                <div class="empty-desc">${escapeHtml(msg)}</div>
                <button class="empty-btn" onclick="handleRefresh()">Обновить</button>
            </div>
        `;
    }

    function renderErrorState(message) {
        const list = document.getElementById('list');
        list.innerHTML = `
            <div class="empty-box">
                <div class="empty-title">Не удалось загрузить</div>
                <div class="empty-desc">${escapeHtml(message)}</div>
                <button class="empty-btn" onclick="handleRefresh()">Повторить</button>
            </div>
        `;
    }

    function renderAuthRequiredState() {
        const list = document.getElementById('list');
        list.innerHTML = `
            <div class="empty-box">
                <div class="empty-title">Регистрация в клубе</div>
                <div class="empty-desc">Для записи на турниры и просмотра рейтинга необходимо подтвердить участие в клубе.</div>
                <button class="empty-btn" onclick="openTermsModal()">Зарегистрироваться</button>
            </div>
        `;
    }

    function escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    function toggleMockSource() {
        allTournaments = sortTournaments(mockTournaments);
        renderTournaments(allTournaments);
        updateStats(allTournaments);
        renderAdminTournaments();
        populateTvTournamentSelect();
        showToast('Данные обновлены');
    }

    setInterval(() => {
        if (tvState.running && tvState.secondsLeft > 0) {
            tvState.secondsLeft--;
            document.getElementById('tvRemoteTimerDisplay').textContent = formatTime(tvState.secondsLeft);
        }
    }, 1000);

    function initEventListeners() {
        const bind = (id, event, handler) => {
            const el = document.getElementById(id);
            if (el) el.addEventListener(event, handler);
        };

        bind('btnPreviewToggleMock', 'click', toggleMockSource);
        bind('tabBtnTournaments', 'click', () => switchMainTab('tournaments'));
        bind('tabBtnLeaderboard', 'click', () => switchMainTab('leaderboard'));
        bind('tabBtnAdmin', 'click', () => switchMainTab('admin'));
        bind('searchInput', 'input', filterTournaments);
        bind('refreshBtn', 'click', handleRefresh);
        bind('btnStartSeason', 'click', openStartSeasonModal);
        bind('btnEndSeason', 'click', handleEndSeason);
        bind('btnPeriodSeason', 'click', () => switchLeaderboardPeriod('season'));
        bind('btnPeriodMonth', 'click', () => switchLeaderboardPeriod('month'));
        bind('btnPeriodWeek', 'click', () => switchLeaderboardPeriod('week'));
        bind('btnSyncTvRemote', 'click', syncTvRemoteFromServer);
        bind('btnApplyTourneyToTv', 'click', applySelectedTournamentToTv);
        bind('btnOpenTvTab', 'click', openTvTab);
        bind('btnCopyTvLink', 'click', copyTvLink);
        bind('tvRemoteBtnPlay', 'click', tvRemoteTogglePlay);
        bind('btnTvApplyBlinds', 'click', handleApplyBlinds);
        const bbInput = document.getElementById('tvInputBigBlind');
        if (bbInput) {
            bbInput.addEventListener('input', () => updateBlindsCalcPreview(bbInput.value));
            bbInput.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    handleApplyBlinds();
                }
            });
        }
        bind('btnTvRemotePrev', 'click', () => tvRemoteAction('PREV_LEVEL'));
        bind('btnTvRemoteNext', 'click', () => tvRemoteAction('NEXT_LEVEL'));
        bind('btnTvRemoteBreak', 'click', () => tvRemoteAction('TOGGLE_BREAK'));
        bind('btnTvRemotePlus1m', 'click', () => tvRemoteAction('ADJUST_TIME', { deltaSeconds: 60 }));
        bind('btnTvRemoteMinus1m', 'click', () => tvRemoteAction('ADJUST_TIME', { deltaSeconds: -60 }));
        bind('btnTvRemoteSync15m', 'click', () => tvRemoteAction('SYNC', { secondsLeft: 20 * 60 }));
        bind('btnOpenCreateTourney', 'click', openCreateModal);
        bind('tournamentModal', 'click', closeModalOnBackdrop);
        bind('btnCloseTournamentModal', 'click', closeTournamentModal);
        bind('tournamentForm', 'submit', handleSaveTournament);
        bind('participantsModal', 'click', closeParticipantsModalOnBackdrop);
        bind('btnCloseParticipantsModal', 'click', closeParticipantsModal);
        bind('btnCloseParticipantsModalBottom', 'click', closeParticipantsModal);
        bind('seasonModal', 'click', closeSeasonModalOnBackdrop);
        bind('btnCloseSeasonModal', 'click', closeSeasonModal);
        bind('seasonForm', 'submit', handleStartSeasonSubmit);
        bind('resultsModal', 'click', closeResultsModalOnBackdrop);
        bind('btnCloseResultsModal', 'click', closeResultsModal);
        bind('btnDownloadTourneyExcelTemplate', 'click', () => downloadTournamentExcel(selectedResultsTourneyId));
        bind('btnResultsFromUrl', 'click', submitResultsFromUrl);
        bind('btnResultsUploadFile', 'click', submitResultsFile);

        const termsCb = document.getElementById('agreeTermsCheckbox');
        if (termsCb) {
            termsCb.addEventListener('change', () => {
                const btn = document.getElementById('btnConfirmRegistration');
                if (btn) btn.disabled = !termsCb.checked;
            });
        }
        bind('btnConfirmRegistration', 'click', handleConfirmRegistration);
    }

    initEventListeners();
    loadUserProfile();
    loadTournaments();
    syncTvRemoteFromServer();
