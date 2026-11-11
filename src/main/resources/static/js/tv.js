
        const DEFAULT_BLIND_STRUCTURE = [
            { level: 1,  sb: 100,   bb: 100,    ante: 100,    minutes: 15, isBreak: false },
            { level: 2,  sb: 100,   bb: 200,    ante: 200,    minutes: 15, isBreak: false },
            { level: 3,  sb: 150,   bb: 300,    ante: 300,    minutes: 15, isBreak: false },
            { level: 4,  sb: 200,   bb: 400,    ante: 400,    minutes: 15, isBreak: false },
            { level: 5,  sb: 0,     bb: 0,      ante: 0,      minutes: 5,  isBreak: true  },
            { level: 6,  sb: 300,   bb: 600,    ante: 600,    minutes: 15, isBreak: false },
            { level: 7,  sb: 400,   bb: 800,    ante: 800,    minutes: 15, isBreak: false },
            { level: 8,  sb: 500,   bb: 1000,   ante: 1000,   minutes: 15, isBreak: false },
            { level: 9,  sb: 600,   bb: 1200,   ante: 1200,   minutes: 15, isBreak: false },
            { level: 10, sb: 0,     bb: 0,      ante: 0,      minutes: 5,  isBreak: true  },
            { level: 11, sb: 800,   bb: 1600,   ante: 1600,   minutes: 15, isBreak: false },
            { level: 12, sb: 1000,  bb: 2000,   ante: 2000,   minutes: 15, isBreak: false },
            { level: 13, sb: 1200,  bb: 2400,   ante: 2400,   minutes: 15, isBreak: false },
            { level: 14, sb: 1500,  bb: 3000,   ante: 3000,   minutes: 15, isBreak: false },
            { level: 15, sb: 0,     bb: 0,      ante: 0,      minutes: 15, isBreak: true  },
            { level: 16, sb: 2000,  bb: 4000,   ante: 4000,   minutes: 15, isBreak: false },
            { level: 17, sb: 3000,  bb: 6000,   ante: 6000,   minutes: 15, isBreak: false },
            { level: 18, sb: 4000,  bb: 8000,   ante: 8000,   minutes: 15, isBreak: false },
            { level: 19, sb: 5000,  bb: 10000,  ante: 10000,  minutes: 15, isBreak: false },
            { level: 20, sb: 0,     bb: 0,      ante: 0,      minutes: 5,  isBreak: true  },
            { level: 21, sb: 6000,  bb: 12000,  ante: 12000,  minutes: 15, isBreak: false },
            { level: 22, sb: 8000,  bb: 16000,  ante: 16000,  minutes: 15, isBreak: false },
            { level: 23, sb: 10000, bb: 20000,  ante: 20000,  minutes: 15, isBreak: false },
            { level: 24, sb: 15000, bb: 30000,  ante: 30000,  minutes: 15, isBreak: false },
            { level: 25, sb: 0,     bb: 0,      ante: 0,      minutes: 5,  isBreak: true  },
            { level: 26, sb: 20000, bb: 40000,  ante: 40000,  minutes: 15, isBreak: false },
            { level: 27, sb: 30000, bb: 60000,  ante: 60000,  minutes: 15, isBreak: false },
            { level: 28, sb: 40000, bb: 80000,  ante: 80000,  minutes: 15, isBreak: false },
            { level: 29, sb: 50000, bb: 100000, ante: 100000, minutes: 15, isBreak: false }
        ];

        let state = {
            currentLevelIdx: 0,
            secondsLeft: 15 * 60,
            isRunning: false,
            timerInterval: null,
            blindStructure: DEFAULT_BLIND_STRUCTURE,
            tournamentTitle: "MAIN EVENT • DEEPSTACK",
            excelUrl: "",
            startingStack: 50000,
            players: [],
            rebuys: [],
            leaders: []
        };

        function generateDemoData() {
            const names = [
                "Алексей Смирнов", "Михаил Петров", "Дмитрий Волков", "Артем Соколов",
                "Сергей Новиков", "Иван Морозов", "Павел Кузнецов", "Никита Попов",
                "Евгений Васильев", "Максим Федоров", "Денис Лебедев", "Роман Семенов",
                "Владислав Егоров", "Андрей Козлов", "Кирилл Степанов", "Илья Николаев",
                "Антон Макаров", "Виктор Захаров", "Константин Зайцев", "Тимофей Соловьев",
                "Валерий Борисов", "Глеб Яковлев", "Олег Григорьев", "Руслан Романов"
            ];

            const players = [];
            const totalEntries = 54;
            const remainingCount = 24;

            for (let i = 0; i < remainingCount; i++) {
                players.push({
                    name: names[i] || `Игрок #${i+1}`,
                    status: "В игре",
                    chips: 0
                });
            }

            for (let i = remainingCount; i < totalEntries; i++) {
                players.push({
                    name: `Игрок #${i+1}`,
                    status: "Выбыл",
                    chips: 0
                });
            }

            const rebuys = [
                { name: "Алексей Смирнов", chips: 100000 },
                { name: "Михаил Петров", chips: 100000 },
                { name: "Дмитрий Волков", chips: 100000 },
                { name: "Артем Соколов", chips: 100000 },
                { name: "Сергей Новиков", chips: 100000 },
                { name: "Иван Морозов", chips: 100000 }
            ];

            const leaders = [
                { name: "Алексей Смирнов", chips: 480000 },
                { name: "Михаил Петров", chips: 410000 },
                { name: "Дмитрий Волков", chips: 360000 },
                { name: "Артем Соколов", chips: 310000 },
                { name: "Сергей Новиков", chips: 270000 }
            ];

            return { players, rebuys, leaders };
        }

        function getCurrentLevel() {
            return state.blindStructure[state.currentLevelIdx] || state.blindStructure[0];
        }

        function getNextLevel() {
            return state.blindStructure[state.currentLevelIdx + 1] || null;
        }

        function formatTime(seconds) {
            const m = Math.floor(seconds / 60);
            const s = seconds % 60;
            return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
        }

        function formatChips(num) {
            if (!num && num !== 0) return "0";
            return num.toLocaleString('ru-RU');
        }

        function updateClockDisplay() {
            const current = getCurrentLevel();
            const next = getNextLevel();
            const timerElem = document.getElementById("timerClockDisplay");
            const progressBar = document.getElementById("timerProgressBar");
            const statusBadge = document.getElementById("stageStatusBadge");
            const statusText = document.getElementById("stageStatusText");

            timerElem.textContent = formatTime(state.secondsLeft);

            timerElem.classList.remove("warning-time", "danger-time");
            if (state.secondsLeft <= 30) {
                timerElem.classList.add("danger-time");
            } else if (state.secondsLeft <= 60) {
                timerElem.classList.add("warning-time");
            }

            const totalSecs = current.minutes * 60;
            const elapsed = totalSecs - state.secondsLeft;
            const pct = Math.min(100, Math.max(0, (elapsed / totalSecs) * 100));
            progressBar.style.width = `${pct}%`;

            statusBadge.classList.remove("break-mode", "paused-mode");
            if (current.isBreak) {
                statusBadge.classList.add("break-mode");
                progressBar.classList.add("break-bar");
                statusText.textContent = "ПЕРЕРЫВ";
                document.getElementById("currentLevelDisplay").textContent = "ПЕРЕРЫВ";
                document.getElementById("valSmallBlind").textContent = "—";
                document.getElementById("valBigBlind").textContent = "—";
                document.getElementById("valAnte").textContent = "—";
            } else {
                progressBar.classList.remove("break-bar");
                if (!state.isRunning) {
                    statusBadge.classList.add("paused-mode");
                    statusText.textContent = "ПАУЗА";
                } else {
                    statusText.textContent = "РАЗЫГРОВКА";
                }
                const sbVal = (state.customSmallBlind !== undefined && state.customSmallBlind !== null) ? state.customSmallBlind : current.sb;
                const bbVal = (state.customBigBlind !== undefined && state.customBigBlind !== null) ? state.customBigBlind : current.bb;
                const anteVal = (state.customAnte !== undefined && state.customAnte !== null) ? state.customAnte : current.ante;
                document.getElementById("currentLevelDisplay").textContent = current.level;
                document.getElementById("valSmallBlind").textContent = formatChips(sbVal);
                document.getElementById("valBigBlind").textContent = formatChips(bbVal);
                document.getElementById("valAnte").textContent = formatChips(anteVal);
            }

            if (next) {
                if (next.isBreak) {
                    document.getElementById("valNextLevel").textContent = `ПЕРЕРЫВ (${next.minutes} мин)`;
                } else {
                    document.getElementById("valNextLevel").textContent =
                        `${formatChips(next.sb)} / ${formatChips(next.bb)} • ANTE ${formatChips(next.ante)}`;
                }
            } else {
                document.getElementById("valNextLevel").textContent = "ФИНАЛ";
            }

            document.getElementById("totalLevelsDisplay").textContent = state.blindStructure.length;
        }

        function startTimer() {
            if (state.isRunning) return;
            state.isRunning = true;
            updateClockDisplay();

            state.timerInterval = setInterval(() => {
                if (state.secondsLeft > 0) {
                    state.secondsLeft--;
                    updateClockDisplay();
                    if (state.secondsLeft === 0) {
                        showToast("Уровень завершен!");
                        nextLevel();
                    }
                }
            }, 1000);
        }

        function pauseTimer() {
            state.isRunning = false;
            clearInterval(state.timerInterval);
            updateClockDisplay();
        }

        function togglePlayPause() {
            if (state.isRunning) {
                pauseTimer();
                showToast("Пауза");
            } else {
                startTimer();
                showToast("Старт таймера");
            }
        }

        function nextLevel() {
            if (state.currentLevelIdx < state.blindStructure.length - 1) {
                state.currentLevelIdx++;
                state.secondsLeft = getCurrentLevel().minutes * 60;
                updateClockDisplay();
                updateTournamentMetrics();
            } else {
                pauseTimer();
                showToast("Турнир подошел к концу!");
            }
        }

        function prevLevel() {
            if (state.currentLevelIdx > 0) {
                state.currentLevelIdx--;
                state.secondsLeft = getCurrentLevel().minutes * 60;
                updateClockDisplay();
                updateTournamentMetrics();
            }
        }

        function escapeHtml(text) {
            const div = document.createElement('div');
            div.textContent = text || '';
            return div.innerHTML;
        }

        function updateTournamentMetrics() {

            const arrivedPlayers = state.players.filter(p => p.arrived || p.status === "В игре" || p.status === "Выбыл");
            const activePlayers = state.players.filter(p => p.status === "В игре");
            const eliminatedPlayers = state.players.filter(p => p.status === "Выбыл");

            const activeCount = activePlayers.length;
            const totalArrived = arrivedPlayers.length > 0 ? arrivedPlayers.length : state.players.length;
            const totalRegistered = state.players.length;
            const eliminatedCount = eliminatedPlayers.length;

            const rebuyCount = state.rebuys.length;
            const rebuyChips = state.rebuys.reduce((sum, r) => sum + (Number(r.chips) || 0), 0);

            let totalChips = 0;
            if (arrivedPlayers.length > 0) {
                totalChips = arrivedPlayers.reduce((sum, p) => sum + (p.chips || 50000), 0) + rebuyChips;
            } else if (state.leaders.length > 0) {
                totalChips = state.leaders.reduce((sum, l) => sum + (Number(l.chips) || 0), 0);
            } else {
                totalChips = (totalArrived * (state.startingStack || 50000)) + rebuyChips;
            }

            const averageStack = activeCount > 0 ? Math.round(totalChips / activeCount) : (totalArrived > 0 ? Math.round(totalChips / totalArrived) : 0);

            const elemRemaining = document.getElementById("kpiRemainingPlayers");
            if (elemRemaining) elemRemaining.textContent = `${activeCount} / ${totalArrived}`;
            const elemRemainingSub = document.getElementById("kpiRemainingSubtext");
            if (elemRemainingSub) {
                if (totalRegistered > totalArrived) {
                    elemRemainingSub.textContent = `Явилось: ${totalArrived} (из ${totalRegistered}) • Выбыло: ${eliminatedCount}`;
                } else {
                    elemRemainingSub.textContent = `Входов: ${totalArrived} • Выбыло: ${eliminatedCount}`;
                }
            }

            const elemRebuys = document.getElementById("kpiTotalRebuys");
            if (elemRebuys) elemRebuys.textContent = `${rebuyCount}`;
            const elemRebuysSub = document.getElementById("kpiRebuysSubtext");
            if (elemRebuysSub) {
                elemRebuysSub.textContent = rebuyCount > 0 ? `+${formatChips(rebuyChips)} фишек` : "0 фишек";
            }

            const elemTotalChips = document.getElementById("kpiTotalChips");
            if (elemTotalChips) elemTotalChips.textContent = formatChips(totalChips);

            const elemAvgStack = document.getElementById("kpiAverageStack");
            if (elemAvgStack) elemAvgStack.textContent = formatChips(averageStack);

            let displayLeaders = [];
            if (state.leaders && state.leaders.length > 0) {
                displayLeaders = state.leaders;
            } else if (activePlayers.length > 0) {
                displayLeaders = activePlayers.map(p => ({
                    name: p.name,
                    chips: p.chips > 0 ? p.chips : averageStack
                }));
            }
            renderTop5Leaderboard(displayLeaders);
        }

        function renderTop5Leaderboard(leaders) {
            const container = document.getElementById("leaderboardContainer");
            if (!container) return;
            container.innerHTML = "";

            const top5 = (leaders || []).slice(0, 5);
            if (top5.length === 0) {
                container.innerHTML = `<div style="color: var(--text-muted); padding: 30px; text-align: center;">Список лидеров пуст</div>`;
                return;
            }

            top5.forEach((player, idx) => {
                const rank = idx + 1;
                const row = document.createElement("div");
                row.className = `leader-row ${rank === 1 ? 'top-1' : rank === 2 ? 'top-2' : rank === 3 ? 'top-3' : ''}`;

                row.innerHTML = `
                    <div class="leader-left">
                        <div class="leader-rank">${rank}</div>
                        <div class="leader-info">
                            <h4>${escapeHtml(player.name)}</h4>
                        </div>
                    </div>
                    <div class="leader-right">
                        <div class="leader-chips">${formatChips(player.chips)}</div>
                    </div>
                `;
                container.appendChild(row);
            });
        }

        function parseCleanNumber(val, defaultVal = 0) {
            if (val === null || val === undefined) return defaultVal;
            const str = String(val).replace(/[^\d]/g, "");
            const num = parseInt(str, 10);
            return isNaN(num) ? defaultVal : num;
        }

        function parseSheet1Players(rows) {
            if (!Array.isArray(rows) || rows.length === 0) return [];
            const parsed = [];
            let startRow = 0;

            const r0 = rows[0] || [];
            let nameCol = 0;
            let statusCol = -1;
            let chipsCol = -1;

            const r0Str = r0.map(c => String(c || "").toLowerCase()).join(" ");
            if (r0Str.includes("игрок") || r0Str.includes("имя") || r0Str.includes("фио") || r0Str.includes("статус") || r0Str.includes("player") || r0Str.includes("telegram") || r0Str.includes("id")) {
                startRow = 1;
                r0.forEach((val, idx) => {
                    const txt = String(val || "").toLowerCase();
                    if (txt.includes("статус") || txt.includes("явк") || txt.includes("status")) {
                        statusCol = idx;
                    } else if (txt.includes("фишк") || txt.includes("стек") || txt.includes("chip") || txt.includes("chips")) {
                        chipsCol = idx;
                    } else if (txt.includes("игрок") || txt.includes("имя") || txt.includes("player") || txt.includes("фио")) {
                        nameCol = idx;
                    }
                });
            }

            for (let i = startRow; i < rows.length; i++) {
                const row = rows[i];
                if (!row) continue;
                const name = String(row[nameCol] || "").trim();
                if (!name || name.length < 2) continue;

                let sCol = statusCol;
                let cCol = chipsCol;
                if (sCol === -1) {
                    sCol = (row.length >= 5) ? 3 : 1;
                }
                if (cCol === -1) {
                    cCol = (row.length >= 5) ? 4 : 2;
                }

                const rawStatus = (sCol !== -1 && row[sCol] !== undefined) ? String(row[sCol]).trim().toLowerCase() : "";

                let arrived = false;
                let status = "Ожидается";

                if (rawStatus.includes("выбыл") || rawStatus.includes("out") || rawStatus.includes("вылетел") || rawStatus.includes("eliminated")) {
                    arrived = true;
                    status = "Выбыл";
                } else if (rawStatus.includes("не явился") || rawStatus.includes("не пришел") || rawStatus.includes("ожидается") ||
                           rawStatus.includes("зарег") || rawStatus.includes("отмена") || rawStatus === "нет" || rawStatus === "-") {
                    arrived = false;
                    status = "Не явился";
                } else if (rawStatus.includes("в игре") || rawStatus.includes("пришел") || rawStatus.includes("играет") ||
                           rawStatus === "да" || rawStatus === "+" || rawStatus.includes("взял фишки") || rawStatus.includes("active")) {
                    arrived = true;
                    status = "В игре";
                } else {
                    let rowChips = (cCol !== -1 && row[cCol] !== undefined) ? parseCleanNumber(row[cCol], 0) : 0;
                    if (rowChips > 0 || rawStatus === "") {
                        arrived = true;
                        status = "В игре";
                    } else {
                        arrived = false;
                        status = "Ожидается";
                    }
                }

                let chips = 0;
                if (cCol !== -1 && row[cCol] !== undefined) {
                    chips = parseCleanNumber(row[cCol], 50000);
                }
                if (chips <= 0) chips = 50000;

                parsed.push({
                    name,
                    status,
                    arrived,
                    chips: arrived ? chips : 0,
                    initialChips: chips
                });
            }
            return parsed;
        }

        function parseSheet2Rebuys(rows) {
            if (!Array.isArray(rows) || rows.length === 0) return [];
            const parsed = [];
            let startRow = 0;

            const r0 = rows[0] || [];
            let nameCol = 0;
            let chipsCol = -1;

            const r0Str = r0.map(c => String(c || "").toLowerCase()).join(" ");
            if (r0Str.includes("игрок") || r0Str.includes("имя") || r0Str.includes("фишк") || r0Str.includes("сумма") || r0Str.includes("ребай") || r0Str.includes("rebuy") || r0Str.includes("id")) {
                startRow = 1;
                r0.forEach((val, idx) => {
                    const txt = String(val || "").toLowerCase();
                    if (txt.includes("фишк") || txt.includes("сумма") || txt.includes("ребай") || txt.includes("rebuy")) {
                        chipsCol = idx;
                    } else if (txt.includes("игрок") || txt.includes("имя") || txt.includes("player")) {
                        nameCol = idx;
                    }
                });
            }

            for (let i = startRow; i < rows.length; i++) {
                const row = rows[i];
                if (!row) continue;
                const name = String(row[nameCol] || "").trim();
                if (!name || name.length < 2) continue;

                let cCol = chipsCol;
                if (cCol === -1) {
                    cCol = (row.length >= 4) ? 3 : 1;
                }

                let chips = parseCleanNumber(row[cCol], 100000);
                if (chips <= 0) chips = 100000;

                parsed.push({ name, chips });
            }
            return parsed;
        }

        function parseSheet3Leaders(rows) {
            if (!Array.isArray(rows) || rows.length === 0) return [];
            const parsed = [];
            let startRow = 0;

            const r0 = rows[0] || [];
            let nameCol = 0;
            let chipsCol = -1;

            const r0Str = r0.map(c => String(c || "").toLowerCase()).join(" ");
            if (r0Str.includes("игрок") || r0Str.includes("имя") || r0Str.includes("лидер") || r0Str.includes("фишк") || r0Str.includes("стек") || r0Str.includes("chips") || r0Str.includes("id")) {
                startRow = 1;
                r0.forEach((val, idx) => {
                    const txt = String(val || "").toLowerCase();
                    if (txt.includes("фишк") || txt.includes("стек") || txt.includes("chip") || txt.includes("chips") || txt.includes("лидер")) {
                        chipsCol = idx;
                    } else if (txt.includes("игрок") || txt.includes("имя") || txt.includes("player")) {
                        nameCol = idx;
                    }
                });
            }

            for (let i = startRow; i < rows.length; i++) {
                const row = rows[i];
                if (!row) continue;
                const name = String(row[nameCol] || "").trim();
                if (!name || name.length < 2) continue;

                let cCol = chipsCol;
                if (cCol === -1) {
                    cCol = (row.length >= 4) ? 3 : 1;
                }

                let chips = parseCleanNumber(row[cCol], 0);
                parsed.push({ name, chips });
            }

            parsed.sort((a, b) => (b.chips || 0) - (a.chips || 0));
            return parsed;
        }

        function parseSheet4Eliminated(rows) {
            if (!Array.isArray(rows) || rows.length === 0) return [];
            const parsed = [];
            let startRow = 0;
            const r0 = rows[0] || [];
            const headerStr = r0.map(c => String(c || "").toLowerCase()).join(" ");
            if (headerStr.includes("id") || headerStr.includes("айди") || headerStr.includes("игрок") || headerStr.includes("ник") || headerStr.includes("name")) {
                startRow = 1;
            }

            for (let i = startRow; i < rows.length; i++) {
                const row = rows[i];
                if (!row) continue;
                let id = null;
                let name = null;
                for (let c = 0; c < row.length; c++) {
                    const val = String(row[c] || "").trim();
                    const num = Number(val.replace(/[^0-9]/g, ''));
                    if (!id && num > 1000) {
                        id = num;
                    } else if (!name && val.length > 1 && isNaN(Number(val))) {
                        name = val;
                    }
                }
                if (id || name) {
                    parsed.push({
                        id: id,
                        name: name || ("Игрок #" + (id || (i + 1))),
                        eliminationOrder: i + 1
                    });
                }
            }
            return parsed;
        }

        function parseMultiSheetWorkbook(workbook, isSilent = false) {
            if (!workbook || !workbook.SheetNames || workbook.SheetNames.length === 0) {
                if (!isSilent) showToast("Таблица пуста");
                return;
            }

            const sheetNames = workbook.SheetNames;
            const norm = (s) => String(s || "").trim().toLowerCase();

            let sheet1 = workbook.Sheets[sheetNames[0]];
            let sheet2 = sheetNames.length > 1 ? workbook.Sheets[sheetNames[1]] : null;
            let sheet3 = sheetNames.length > 2 ? workbook.Sheets[sheetNames[2]] : null;
            let sheet4 = sheetNames.length > 3 ? workbook.Sheets[sheetNames[3]] : null;

            for (const name of sheetNames) {
                const n = norm(name);
                if (n.includes("вход") || n.includes("игрок") || n.includes("buy")) {
                    sheet1 = workbook.Sheets[name];
                } else if (n.includes("перевыкуп") || n.includes("реба") || n.includes("rebuy") || n.includes("выкуп")) {
                    sheet2 = workbook.Sheets[name];
                } else if (n.includes("лидер") || n.includes("leader") || n.includes("топ") || n.includes("чип") || n.includes("стек")) {
                    sheet3 = workbook.Sheets[name];
                } else if (n.includes("выбыв") || n.includes("out") || n.includes("итог") || n.includes("eliminated")) {
                    sheet4 = workbook.Sheets[name];
                }
            }

            const rows1 = XLSX.utils.sheet_to_json(sheet1, { header: 1 });
            let parsedPlayers = parseSheet1Players(rows1);

            let parsedRebuys = [];
            if (sheet2) {
                const rows2 = XLSX.utils.sheet_to_json(sheet2, { header: 1 });
                parsedRebuys = parseSheet2Rebuys(rows2);
            }

            let parsedLeaders = [];
            if (sheet3) {
                const rows3 = XLSX.utils.sheet_to_json(sheet3, { header: 1 });
                parsedLeaders = parseSheet3Leaders(rows3);
            }

            let parsedEliminated = [];
            if (sheet4) {
                const rows4 = XLSX.utils.sheet_to_json(sheet4, { header: 1 });
                parsedEliminated = parseSheet4Eliminated(rows4);
            }

            if (parsedEliminated.length > 0) {
                const eliminatedNames = new Set(parsedEliminated.map(p => p.name.toLowerCase().trim()));
                const eliminatedIds = new Set(parsedEliminated.filter(p => p.id).map(p => String(p.id)));

                parsedPlayers.forEach(p => {
                    const pName = String(p.name || "").toLowerCase().trim();
                    const pId = p.id ? String(p.id) : null;
                    if (eliminatedNames.has(pName) || (pId && eliminatedIds.has(pId))) {
                        p.status = "Выбыл";
                    } else {
                        p.status = "В игре";
                    }
                });
            }

            state.players = parsedPlayers;
            state.rebuys = parsedRebuys;
            state.leaders = parsedLeaders;
            state.eliminated = parsedEliminated;

            updateTournamentMetrics();
            if (!isSilent) {
                showToast(`Загружено: ${parsedPlayers.length} входов, ${parsedRebuys.length} ребаев, ${parsedLeaders.length} лидеров`);
            }
        }

        function handleFileBuffer(arrayBuffer, isSilent = false) {
            try {
                if (typeof XLSX === 'undefined') {
                    if (!isSilent) showToast("Библиотека XLSX недоступна");
                    return;
                }
                const workbook = XLSX.read(arrayBuffer, { type: 'array' });
                parseMultiSheetWorkbook(workbook, isSilent);
            } catch (err) {
                console.error(err);
                if (!isSilent) showToast("Ошибка при чтении таблицы: " + err.message);
            }
        }

        async function loadFromUrl(url, isSilent = false) {
            if (!url || !url.trim()) return;
            url = url.trim();

            if (url.includes("docs.google.com/spreadsheets")) {
                const match = url.match(/\/d\/([a-zA-Z0-9-_]+)/);
                if (match && match[1]) {
                    url = `https://docs.google.com/spreadsheets/d/${match[1]}/export?format=xlsx`;
                }
            }

            const badge = document.getElementById("liveSyncBadge");
            if (badge) badge.classList.add("syncing");

            if (!isSilent) {
                showToast("Загрузка таблицы...");
            }

            try {
                let res;
                if (url.startsWith("/") || url.startsWith("./") || (!url.startsWith("http://") && !url.startsWith("https://"))) {
                    const localUrl = url.includes("?") ? `${url}&t=${Date.now()}` : `${url}?t=${Date.now()}`;
                    res = await fetch(localUrl);
                } else {
                    let proxyUrl = `/api/public/proxy-excel?url=${encodeURIComponent(url)}`;
                    res = await fetch(proxyUrl);
                    if (!res.ok && res.status === 404) {
                        proxyUrl = `/public/proxy-excel?url=${encodeURIComponent(url)}`;
                        res = await fetch(proxyUrl);
                    }
                }

                if (!res.ok) {
                    let errMsg = "";
                    try {
                        const errJson = await res.json();
                        errMsg = errJson.message || errJson.error || "";
                    } catch (e) {
                        errMsg = await res.text();
                    }
                    if (res.status === 401 || res.status === 403 || errMsg.includes("приватности") || errMsg.includes("доступ")) {
                        throw new Error("Google Таблица закрыта. Включите: «Поделиться» -> «Все, у кого есть ссылка: Читатель».");
                    }
                    throw new Error(errMsg || ("HTTP " + res.status));
                }

                const buffer = await res.arrayBuffer();
                handleFileBuffer(buffer, isSilent);
                state.excelUrl = url;
            } catch (e) {
                console.error(e);
                if (!isSilent) {
                    showToast("Ошибка загрузки: " + e.message);
                }
            } finally {
                if (badge) {
                    setTimeout(() => badge.classList.remove("syncing"), 1200);
                }
            }
        }

        let toastTimeout = null;
        function showToast(msg) {
            const toast = document.getElementById("toastMsg");
            toast.textContent = msg;
            toast.classList.add("show");
            clearTimeout(toastTimeout);
            toastTimeout = setTimeout(() => {
                toast.classList.remove("show");
            }, 2600);
        }

        document.addEventListener("DOMContentLoaded", () => {

            setInterval(() => {
                const now = new Date();
                document.getElementById("liveWallClock").textContent = now.toLocaleTimeString('ru-RU');
            }, 1000);

            const demo = generateDemoData();
            state.players = demo.players;
            state.rebuys = demo.rebuys;
            state.leaders = demo.leaders;
            state.secondsLeft = getCurrentLevel().minutes * 60;
            updateClockDisplay();
            updateTournamentMetrics();

            document.getElementById("pokerClockCard").addEventListener("click", togglePlayPause);

            document.getElementById("btnFullscreenToggle").addEventListener("click", () => {
                if (!document.fullscreenElement) {
                    document.documentElement.requestFullscreen().catch(() => {});
                    showToast("Полноэкранный режим");
                } else {
                    document.exitFullscreen().catch(() => {});
                }
            });

            const modal = document.getElementById("modalSettings");
            const btnSettingsOpen = document.getElementById("btnSettingsOpen");
            if (btnSettingsOpen && modal) {
                btnSettingsOpen.addEventListener("click", () => {
                    modal.classList.add("active");
                });
            }
            const btnSettingsClose = document.getElementById("btnSettingsClose");
            if (btnSettingsClose && modal) {
                btnSettingsClose.addEventListener("click", () => {
                    modal.classList.remove("active");
                });
            }
            if (modal) {
                modal.addEventListener("click", (e) => {
                    if (e.target === modal) modal.classList.remove("active");
                });
            }

            document.getElementById("btnModalPlayPause").addEventListener("click", togglePlayPause);
            document.getElementById("btnModalPrev").addEventListener("click", prevLevel);
            document.getElementById("btnModalNext").addEventListener("click", nextLevel);
            document.getElementById("btnModalBreak").addEventListener("click", () => {
                const cur = getCurrentLevel();
                cur.isBreak = !cur.isBreak;
                updateClockDisplay();
                updateTournamentMetrics();
                showToast(cur.isBreak ? "Включен режим ПЕРЕРЫВ" : "Включен режим РАЗЫГРОВКА");
            });
            document.getElementById("btnModalPlus").addEventListener("click", () => {
                state.secondsLeft += 60;
                updateClockDisplay();
            });
            document.getElementById("btnModalMinus").addEventListener("click", () => {
                state.secondsLeft = Math.max(0, state.secondsLeft - 60);
                updateClockDisplay();
            });

            document.getElementById("inputTournamentTitle").addEventListener("change", (e) => {
                state.tournamentTitle = e.target.value.trim() || "POKER TOURNAMENT";
                document.getElementById("tvTournamentTitle").textContent = state.tournamentTitle;
            });

            document.getElementById("btnLoadFromUrl").addEventListener("click", () => {
                const url = document.getElementById("inputExcelUrl").value.trim();
                if (url) {
                    loadFromUrl(url);
                    modal.classList.remove("active");
                }
            });

            const dropZone = document.getElementById("dropZone");
            const filePicker = document.getElementById("filePicker");
            dropZone.addEventListener("click", () => filePicker.click());
            dropZone.addEventListener("dragover", (e) => {
                e.preventDefault();
                dropZone.classList.add("dragover");
            });
            dropZone.addEventListener("dragleave", () => dropZone.classList.remove("dragover"));
            dropZone.addEventListener("drop", (e) => {
                e.preventDefault();
                dropZone.classList.remove("dragover");
                if (e.dataTransfer.files && e.dataTransfer.files[0]) {
                    const file = e.dataTransfer.files[0];
                    file.arrayBuffer().then(buf => handleFileBuffer(buf));
                    modal.classList.remove("active");
                }
            });
            filePicker.addEventListener("change", (e) => {
                if (e.target.files && e.target.files[0]) {
                    const file = e.target.files[0];
                    file.arrayBuffer().then(buf => handleFileBuffer(buf));
                    modal.classList.remove("active");
                }
            });

            document.getElementById("btnLoadDemo").addEventListener("click", () => {
                loadFromUrl("/demo_tournament.xlsx");
                modal.classList.remove("active");
                showToast("Демо-таблица (3 листа) загружена");
            });

            document.getElementById("btnFetchFromBackend").addEventListener("click", async () => {
                try {
                    const res = await fetch("/api/public/tournaments");
                    if (!res.ok) throw new Error("HTTP " + res.status);
                    const list = await res.json();
                    if (list && list.length > 0) {
                        const first = list[0];
                        document.getElementById("inputTournamentTitle").value = first.title;
                        state.tournamentTitle = first.title;
                        document.getElementById("tvTournamentTitle").textContent = first.title;
                        if (first.excelUrl) {
                            document.getElementById("inputExcelUrl").value = first.excelUrl;
                            loadFromUrl(first.excelUrl);
                        } else {
                            showToast(`Загружен турнир «${first.title}»`);
                        }
                        modal.classList.remove("active");
                    } else {
                        showToast("В базе пока нет турниров");
                    }
                } catch (err) {
                    showToast("Ошибка связи с сервером: " + err.message);
                }
            });

            const urlParams = new URLSearchParams(window.location.search);
            const tourId = urlParams.get("tournamentId");
            const excelParam = urlParams.get("excelUrl");

            if (tourId) {
                fetch(`/api/public/tournaments/${tourId}`)
                    .then(r => r.json())
                    .then(t => {
                        if (t && t.title) {
                            state.tournamentTitle = t.title;
                            document.getElementById("tvTournamentTitle").textContent = t.title;
                            document.getElementById("inputTournamentTitle").value = t.title;
                            if (t.excelUrl) {
                                state.excelUrl = t.excelUrl;
                                document.getElementById("inputExcelUrl").value = t.excelUrl;
                                loadFromUrl(t.excelUrl);
                            }
                        }
                    })
                    .catch(e => console.warn(e));
            } else if (excelParam) {
                state.excelUrl = excelParam;
                document.getElementById("inputExcelUrl").value = excelParam;
                loadFromUrl(excelParam);
            } else {

                loadFromUrl("/demo_tournament.xlsx");
            }

            let lastServerTournamentId = null;
            async function syncTvWithServer() {
                try {
                    const res = await fetch('/api/public/tv/state');
                    if (!res.ok) return;
                    const server = await res.json();
                    if (!server) return;

                    if (server.tournamentId && server.tournamentId !== lastServerTournamentId) {
                        lastServerTournamentId = server.tournamentId;
                        if (server.tournamentTitle) {
                            state.tournamentTitle = server.tournamentTitle;
                            document.getElementById("tvTournamentTitle").textContent = server.tournamentTitle;
                            document.getElementById("inputTournamentTitle").value = server.tournamentTitle;
                        }
                        if (server.excelUrl && server.excelUrl !== state.excelUrl) {
                            state.excelUrl = server.excelUrl;
                            document.getElementById("inputExcelUrl").value = server.excelUrl;
                            loadFromUrl(server.excelUrl);
                        }
                    }

                    if (server.customStructure && Array.isArray(server.customStructure) && server.customStructure.length > 0) {
                        state.blindStructure = server.customStructure;
                        document.getElementById("totalLevelsDisplay").textContent = state.blindStructure.length;
                    } else if (state.blindStructure !== DEFAULT_BLIND_STRUCTURE) {
                        state.blindStructure = DEFAULT_BLIND_STRUCTURE;
                        document.getElementById("totalLevelsDisplay").textContent = state.blindStructure.length;
                    }

                    if (server.currentLevel && server.currentLevel > 0 && server.currentLevel <= state.blindStructure.length) {
                        const targetIdx = server.currentLevel - 1;
                        if (state.currentLevelIdx !== targetIdx) {
                            state.currentLevelIdx = targetIdx;
                            updateClockDisplay();
                            updateTournamentMetrics();
                        }
                    }

                    const cur = getCurrentLevel();
                    const isBreak = server.break !== undefined ? !!server.break : !!server.isBreak;
                    if (cur.isBreak !== isBreak) {
                        cur.isBreak = isBreak;
                        updateClockDisplay();
                        updateTournamentMetrics();
                    }

                    const isRunning = server.running !== undefined ? !!server.running : !!server.isRunning;
                    if (isRunning && !state.isRunning) {
                        startTimer();
                    } else if (!isRunning && state.isRunning) {
                        pauseTimer();
                    }

                    if (server.secondsLeft !== undefined && Math.abs(server.secondsLeft - state.secondsLeft) > 2) {
                        state.secondsLeft = server.secondsLeft;
                        updateClockDisplay();
                    }

                    if (server.bigBlind !== undefined && server.bigBlind !== null) {
                        if (state.customBigBlind !== server.bigBlind || state.customSmallBlind !== server.smallBlind || state.customAnte !== server.ante) {
                            state.customBigBlind = server.bigBlind;
                            state.customSmallBlind = server.smallBlind;
                            state.customAnte = server.ante;
                            updateClockDisplay();
                        }
                    }
                } catch (e) {}
            }

            setInterval(syncTvWithServer, 2000);
            syncTvWithServer();

            setInterval(() => {
                if (state.excelUrl) {
                    loadFromUrl(state.excelUrl, true);
                }
            }, 20000);

            window.addEventListener("keydown", (e) => {
                if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return;
                if (e.code === "Space") {
                    e.preventDefault();
                    togglePlayPause();
                } else if (e.code === "ArrowRight") {
                    e.preventDefault();
                    nextLevel();
                } else if (e.code === "ArrowLeft") {
                    e.preventDefault();
                    prevLevel();
                } else if (e.key === "f" || e.key === "F") {
                    e.preventDefault();
                    document.getElementById("btnFullscreenToggle").click();
                }
            });
        });
