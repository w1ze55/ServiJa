import fs from "node:fs";
import fsp from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { spawnSync } from "node:child_process";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const ROOT_DIR = path.resolve(__dirname, "..", "..");
const DEFAULT_CONFIG_PATH = path.join(__dirname, "config.local.json");
const REPORTS_DIR = path.join(__dirname, "reports");

async function main() {
  const cli = parseArgs(process.argv.slice(2));

  if (cli.help) {
    printHelp();
    return;
  }

  const config = loadConfig(cli.configPath ?? DEFAULT_CONFIG_PATH);
  const availableProfiles = getAvailableProfiles(config);
  const envFileValues = loadDotEnv(path.join(ROOT_DIR, ".env"));
  const credentials = resolveAdminCredentials(envFileValues);
  const baseUrl = normalizeBaseUrl(process.env.LOAD_BASE_URL ?? config.baseUrl);
  const selectedProfiles = resolveProfiles(cli.profile, availableProfiles);
  const runLabel = buildRunLabel(selectedProfiles, availableProfiles);
  const runDir = path.join(REPORTS_DIR, `${timestampForFileName()}-${runLabel}`);

  await fsp.mkdir(runDir, { recursive: true });

  const context = {
    rootDir: ROOT_DIR,
    baseUrl,
    config,
    credentials,
    runDir,
    cli,
    adminAuth: {
      token: null,
      refreshPromise: null
    }
  };

  if (cli.preflightOnly) {
    const results = [];

    for (const profileName of selectedProfiles) {
      const preflight = await runPreflight(context, profileName);
      results.push({
        profile: profileName,
        preflight
      });
    }

    const payload = {
      generatedAt: new Date().toISOString(),
      baseUrl,
      mode: "preflight-only",
      results
    };

    await writeJson(path.join(runDir, "preflight.json"), payload);
    await writeText(path.join(runDir, "summary.md"), renderPreflightMarkdown(payload));
    console.log(`Preflight complete. Report written to ${runDir}`);
    return;
  }

  const results = [];
  for (let index = 0; index < selectedProfiles.length; index += 1) {
    const profileName = selectedProfiles[index];
    console.log(`Starting profile ${profileName}...`);
    const result = await runProfile(context, profileName);
    results.push(result);
    await writeJson(path.join(runDir, `profile-${profileName}.json`), result);

    if (index < selectedProfiles.length - 1) {
      console.log(`Waiting ${config.intervalBetweenProfilesMs}ms before the next profile...`);
      await sleep(config.intervalBetweenProfilesMs);
    }
  }

  const summary = {
    generatedAt: new Date().toISOString(),
    baseUrl,
    mode: "load",
    selectedProfiles,
    config: {
      timeoutMs: config.timeoutMs,
      thinkTimeMs: config.thinkTimeMs,
      mix: config.mix,
      intervalBetweenProfilesMs: config.intervalBetweenProfilesMs
    },
    results,
    comparison: buildComparison(results)
  };

  await writeJson(path.join(runDir, "summary.json"), summary);
  await writeText(path.join(runDir, "summary.md"), renderSummaryMarkdown(summary));
  console.log(`Load test suite finished. Reports written to ${runDir}`);
}

function parseArgs(argv) {
  const cli = {
    profile: "all",
    preflightOnly: false,
    help: false,
    configPath: null
  };

  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];

    if (arg === "--help" || arg === "-h") {
      cli.help = true;
      continue;
    }

    if (arg === "--preflight-only") {
      cli.preflightOnly = true;
      continue;
    }

    if (arg === "--profile") {
      const value = argv[index + 1];
      if (!value) {
        throw new Error("Missing value for --profile");
      }
      cli.profile = value;
      index += 1;
      continue;
    }

    if (arg === "--config") {
      const value = argv[index + 1];
      if (!value) {
        throw new Error("Missing value for --config");
      }
      cli.configPath = path.resolve(ROOT_DIR, value);
      index += 1;
      continue;
    }

    throw new Error(`Unknown argument: ${arg}`);
  }

  return cli;
}

function printHelp() {
  console.log([
    "Usage:",
    "  node tests/load/run-load.mjs [--profile 50|100|1000|10000|50000|all|comma,separated,list] [--preflight-only] [--config path]",
    "",
    "Examples:",
    "  node tests/load/run-load.mjs",
    "  node tests/load/run-load.mjs --profile 50",
    "  node tests/load/run-load.mjs --profile 10000,50000",
    "  node tests/load/run-load.mjs --profile all --preflight-only"
  ].join("\n"));
}

function loadConfig(configPath) {
  const raw = fs.readFileSync(configPath, "utf8");
  const config = JSON.parse(raw);

  if (!config.baseUrl) {
    throw new Error("Config is missing baseUrl");
  }

  if (!config.profiles || typeof config.profiles !== "object") {
    throw new Error("Config is missing profiles");
  }

  return config;
}

function getAvailableProfiles(config) {
  const profiles = Object.keys(config.profiles ?? {});

  if (profiles.length === 0) {
    throw new Error("Config must define at least one profile.");
  }

  return profiles.sort((left, right) => {
    const leftNumber = Number(left);
    const rightNumber = Number(right);
    const bothNumeric = Number.isFinite(leftNumber) && Number.isFinite(rightNumber);

    if (bothNumeric) {
      return leftNumber - rightNumber;
    }

    return left.localeCompare(right);
  });
}

function loadDotEnv(envPath) {
  if (!fs.existsSync(envPath)) {
    return {};
  }

  const raw = fs.readFileSync(envPath, "utf8");
  const env = {};

  for (const line of raw.split(/\r?\n/u)) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) {
      continue;
    }

    const separatorIndex = trimmed.indexOf("=");
    if (separatorIndex <= 0) {
      continue;
    }

    const key = trimmed.slice(0, separatorIndex).trim();
    let value = trimmed.slice(separatorIndex + 1).trim();

    if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
      value = value.slice(1, -1);
    }

    env[key] = value;
  }

  return env;
}

function resolveAdminCredentials(envFileValues) {
  const email = process.env.LOAD_ADMIN_EMAIL
    || process.env.SERVIJA_ADMIN_BOOTSTRAP_EMAIL
    || envFileValues.SERVIJA_ADMIN_BOOTSTRAP_EMAIL;
  const password = process.env.LOAD_ADMIN_PASSWORD
    || process.env.SERVIJA_ADMIN_BOOTSTRAP_PASSWORD
    || envFileValues.SERVIJA_ADMIN_BOOTSTRAP_PASSWORD;

  if (!email || !password) {
    throw new Error(
      "Admin credentials were not found. Set LOAD_ADMIN_EMAIL and LOAD_ADMIN_PASSWORD or keep the bootstrap admin values in .env."
    );
  }

  return { email, password };
}

function resolveProfiles(profileArg, availableProfiles) {
  if (!profileArg || profileArg === "all") {
    return [...availableProfiles];
  }

  const requestedProfiles = profileArg
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);

  if (requestedProfiles.length === 0) {
    throw new Error("Missing value for --profile");
  }

  const invalidProfiles = requestedProfiles.filter((profile) => !availableProfiles.includes(profile));
  if (invalidProfiles.length > 0) {
    throw new Error(
      `Invalid profile(s): ${invalidProfiles.join(", ")}. Available: ${availableProfiles.join(", ")}, all.`
    );
  }

  return [...new Set(requestedProfiles)];
}

function buildRunLabel(selectedProfiles, availableProfiles) {
  if (
    selectedProfiles.length === availableProfiles.length
    && selectedProfiles.every((profile, index) => profile === availableProfiles[index])
  ) {
    return "all";
  }

  if (selectedProfiles.length === 1) {
    return `profile-${selectedProfiles[0]}`;
  }

  return `profiles-${selectedProfiles.join("-")}`;
}

function normalizeBaseUrl(baseUrl) {
  return String(baseUrl).replace(/\/+$/u, "");
}

function joinUrl(baseUrl, requestPath) {
  if (/^https?:\/\//iu.test(requestPath)) {
    return requestPath;
  }

  if (!requestPath.startsWith("/")) {
    return `${baseUrl}/${requestPath}`;
  }

  return `${baseUrl}${requestPath}`;
}

function timestampForFileName() {
  const now = new Date();
  const parts = [
    now.getFullYear(),
    pad2(now.getMonth() + 1),
    pad2(now.getDate())
  ];
  const time = [pad2(now.getHours()), pad2(now.getMinutes()), pad2(now.getSeconds())].join("");
  return `${parts.join("")}-${time}`;
}

function pad2(value) {
  return String(value).padStart(2, "0");
}

function createMetricBucket(name) {
  return {
    name,
    requestCount: 0,
    successCount: 0,
    errorCount: 0,
    durationSumMs: 0,
    maxDurationMs: 0,
    durationHistogram: new Map(),
    statuses: new Map()
  };
}

function recordHistogramValue(histogram, durationMs) {
  const roundedDuration = Math.max(0, Math.round(durationMs));
  histogram.set(roundedDuration, (histogram.get(roundedDuration) ?? 0) + 1);
}

function incrementMap(map, key, count = 1) {
  map.set(key, (map.get(key) ?? 0) + count);
}

function ensureEndpointMetric(runtime, endpointKey) {
  if (!runtime.metrics.byEndpoint.has(endpointKey)) {
    runtime.metrics.byEndpoint.set(endpointKey, createMetricBucket(endpointKey));
  }

  return runtime.metrics.byEndpoint.get(endpointKey);
}

function recordRequestMetric(runtime, endpointKey, statusLabel, durationMs, ok) {
  const overall = runtime.metrics.overall;
  const endpoint = ensureEndpointMetric(runtime, endpointKey);

  for (const metric of [overall, endpoint]) {
    metric.requestCount += 1;
    metric.durationSumMs += durationMs;
    metric.maxDurationMs = Math.max(metric.maxDurationMs, durationMs);
    recordHistogramValue(metric.durationHistogram, durationMs);
    incrementMap(metric.statuses, statusLabel);

    if (ok) {
      metric.successCount += 1;
    } else {
      metric.errorCount += 1;
    }
  }

  runtime.recentWindow.requests += 1;
  if (!ok) {
    runtime.recentWindow.errors += 1;
  }
}

function percentileFromHistogram(histogram, totalCount, percentile) {
  if (totalCount === 0) {
    return null;
  }

  const threshold = Math.ceil(totalCount * percentile);
  let seen = 0;
  const entries = [...histogram.entries()].sort((left, right) => left[0] - right[0]);

  for (const [value, count] of entries) {
    seen += count;
    if (seen >= threshold) {
      return value;
    }
  }

  return entries.at(-1)?.[0] ?? null;
}

function summarizeMetric(metric, elapsedMs) {
  const requestCount = metric.requestCount;
  const errorRate = requestCount === 0 ? 0 : metric.errorCount / requestCount;
  const durationAverageMs = requestCount === 0 ? 0 : metric.durationSumMs / requestCount;
  const elapsedSeconds = elapsedMs / 1000;

  return {
    name: metric.name,
    requestCount,
    successCount: metric.successCount,
    errorCount: metric.errorCount,
    errorRate,
    requestsPerSecond: elapsedSeconds > 0 ? requestCount / elapsedSeconds : 0,
    averageMs: durationAverageMs,
    p50Ms: percentileFromHistogram(metric.durationHistogram, requestCount, 0.5),
    p95Ms: percentileFromHistogram(metric.durationHistogram, requestCount, 0.95),
    p99Ms: percentileFromHistogram(metric.durationHistogram, requestCount, 0.99),
    maxMs: metric.maxDurationMs,
    statuses: Object.fromEntries(
      [...metric.statuses.entries()].sort((left, right) => String(left[0]).localeCompare(String(right[0])))
    )
  };
}

async function performRequest(context, requestPath, options = {}) {
  const method = options.method ?? "GET";
  const metricKey = options.metricKey ?? `${method} ${requestPath}`;
  const url = joinUrl(context.baseUrl, requestPath);
  const timeoutMs = options.timeoutMs ?? context.config.timeoutMs;
  const parseAs = options.parseAs ?? "none";
  const measure = options.measure ?? false;
  const headers = {
    Accept: "application/json",
    ...(options.headers ?? {})
  };

  let body;
  if (options.json !== undefined) {
    headers["Content-Type"] = "application/json";
    body = JSON.stringify(options.json);
  } else if (options.body !== undefined) {
    body = options.body;
  }

  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  const startedAt = Date.now();

  try {
    const response = await fetch(url, {
      method,
      headers,
      body,
      signal: controller.signal
    });

    const durationMs = Date.now() - startedAt;
    const ok = response.status >= 200 && response.status < 400;
    let payload = null;

    if (parseAs === "json") {
      payload = await response.json();
    } else if (parseAs === "text") {
      payload = await response.text();
    } else {
      await response.arrayBuffer();
    }

    if (measure) {
      recordRequestMetric(context.runtime, metricKey, String(response.status), durationMs, ok);
    }

    return {
      ok,
      status: response.status,
      durationMs,
      data: payload
    };
  } catch (error) {
    const durationMs = Date.now() - startedAt;
    const errorLabel = error?.name === "AbortError" ? "ERR_TIMEOUT" : "ERR_NETWORK";

    if (measure) {
      recordRequestMetric(context.runtime, metricKey, errorLabel, durationMs, false);
    }

    return {
      ok: false,
      status: null,
      durationMs,
      error,
      errorLabel,
      data: null
    };
  } finally {
    clearTimeout(timer);
  }
}

async function loginAdmin(context, force = false) {
  if (!force && context.adminAuth.token) {
    return context.adminAuth.token;
  }

  if (!force && context.adminAuth.refreshPromise) {
    return context.adminAuth.refreshPromise;
  }

  context.adminAuth.refreshPromise = (async () => {
    const response = await performRequest(context, "/api/auth/admin/login", {
      method: "POST",
      json: {
        email: context.credentials.email,
        password: context.credentials.password
      },
      parseAs: "json",
      measure: false
    });

    if (!response.ok) {
      throw new Error(`Admin login failed with status ${response.status ?? response.errorLabel}`);
    }

    if (!response.data?.token) {
      throw new Error("Admin login did not return a JWT token.");
    }

    if (response.data.requires2fa) {
      throw new Error("Admin login requires 2FA. The load test suite expects the bootstrap admin without 2FA.");
    }

    context.adminAuth.token = response.data.token;
    return context.adminAuth.token;
  })();

  try {
    return await context.adminAuth.refreshPromise;
  } finally {
    context.adminAuth.refreshPromise = null;
  }
}

async function performAdminRequest(context, requestPath, options = {}) {
  const token = await loginAdmin(context);
  let response = await performRequest(context, requestPath, {
    ...options,
    headers: {
      Authorization: `Bearer ${token}`,
      ...(options.headers ?? {})
    }
  });

  if (response.status === 401) {
    const refreshedToken = await loginAdmin(context, true);
    response = await performRequest(context, requestPath, {
      ...options,
      headers: {
        Authorization: `Bearer ${refreshedToken}`,
        ...(options.headers ?? {})
      }
    });
  }

  return response;
}

async function runPreflight(context, profileName) {
  context.adminAuth.token = null;
  const startedAt = Date.now();
  const checks = [];

  const categoriesResponse = await performRequest(context, "/api/categories/public/tree", {
    parseAs: "json",
    measure: false
  });
  assertPreflight(categoriesResponse.ok && categoriesResponse.status === 200, "GET /api/categories/public/tree must return 200.");
  checks.push({
    name: "GET /api/categories/public/tree",
    ok: true,
    status: categoriesResponse.status,
    durationMs: categoriesResponse.durationMs
  });

  const servicesResponse = await performRequest(context, "/api/services/public/all", {
    parseAs: "json",
    measure: false
  });
  assertPreflight(servicesResponse.ok, "GET /api/services/public/all must return 200.");
  assertPreflight(Array.isArray(servicesResponse.data) && servicesResponse.data.length > 0, "GET /api/services/public/all must return at least one service.");
  checks.push({
    name: "GET /api/services/public/all",
    ok: true,
    status: servicesResponse.status,
    durationMs: servicesResponse.durationMs,
    count: servicesResponse.data.length
  });

  const firstService = servicesResponse.data[0];
  assertPreflight(firstService?.id && firstService?.freelancerId, "The first public service must expose id and freelancerId.");

  const serviceDetailResponse = await performRequest(context, `/api/services/public/${firstService.id}`, {
    parseAs: "json",
    measure: false
  });
  assertPreflight(serviceDetailResponse.ok, "GET /api/services/public/{serviceId} must return 200 for the selected fixture.");
  checks.push({
    name: "GET /api/services/public/{serviceId}",
    ok: true,
    status: serviceDetailResponse.status,
    durationMs: serviceDetailResponse.durationMs,
    fixtureServiceId: firstService.id
  });

  const freelancerProfileResponse = await performRequest(context, `/api/freelancer/${firstService.freelancerId}/profile`, {
    parseAs: "json",
    measure: false
  });
  assertPreflight(freelancerProfileResponse.ok, "GET /api/freelancer/{freelancerId}/profile must return 200 for the selected fixture.");
  checks.push({
    name: "GET /api/freelancer/{freelancerId}/profile",
    ok: true,
    status: freelancerProfileResponse.status,
    durationMs: freelancerProfileResponse.durationMs,
    fixtureFreelancerId: firstService.freelancerId
  });

  const freelancerServicesResponse = await performRequest(context, `/api/services/public/freelancer/${firstService.freelancerId}`, {
    parseAs: "json",
    measure: false
  });
  assertPreflight(freelancerServicesResponse.ok, "GET /api/services/public/freelancer/{freelancerId} must return 200 for the selected fixture.");
  checks.push({
    name: "GET /api/services/public/freelancer/{freelancerId}",
    ok: true,
    status: freelancerServicesResponse.status,
    durationMs: freelancerServicesResponse.durationMs
  });

  const loginStartedAt = Date.now();
  const adminToken = await loginAdmin(context, true);
  checks.push({
    name: "POST /api/auth/admin/login",
    ok: true,
    status: 200,
    durationMs: Date.now() - loginStartedAt
  });
  assertPreflight(Boolean(adminToken), "Admin login must return a JWT token.");

  const adminUsersResponse = await performAdminRequest(context, "/api/admin/users?page=0&size=1", {
    parseAs: "json",
    measure: false
  });
  assertPreflight(adminUsersResponse.ok, "GET /api/admin/users?page=0&size=1 must return 200.");
  assertPreflight(
    Array.isArray(adminUsersResponse.data?.content) && adminUsersResponse.data.content.length > 0,
    "GET /api/admin/users?page=0&size=1 must return at least one user."
  );
  checks.push({
    name: "GET /api/admin/users?page=0&size=1",
    ok: true,
    status: adminUsersResponse.status,
    durationMs: adminUsersResponse.durationMs,
    count: adminUsersResponse.data.content.length
  });

  return {
    profile: profileName,
    startedAt: new Date(startedAt).toISOString(),
    finishedAt: new Date().toISOString(),
    durationMs: Date.now() - startedAt,
    fixtures: {
      serviceId: firstService.id,
      freelancerId: firstService.freelancerId,
      adminUserId: adminUsersResponse.data.content[0].id
    },
    checks
  };
}

function assertPreflight(condition, message) {
  if (!condition) {
    throw new Error(`Preflight failed: ${message}`);
  }
}

function createRuntimeContext(context, profileName, preflight) {
  const profileConfig = context.config.profiles[profileName];
  return {
    profileName,
    profileConfig,
    preflight,
    metrics: {
      overall: createMetricBucket("overall"),
      byEndpoint: new Map()
    },
    scenarioIterations: {
      publicBrowse: 0,
      adminRead: 0
    },
    workers: new Map(),
    nextWorkerId: 1,
    shouldAbort: false,
    abortedReason: null,
    recentWindow: {
      requests: 0,
      errors: 0
    }
  };
}

async function runProfile(context, profileName) {
  const preflight = await runPreflight(context, profileName);
  const runtime = createRuntimeContext(context, profileName, preflight);
  context.runtime = runtime;

  const dockerBefore = captureDockerStats(context.config);
  const profileStart = Date.now();
  const totalDurationMs = getProfileDurationMs(runtime.profileConfig);
  const schedulerTickMs = context.config.schedulerTickMs;
  const progressLogIntervalMs = context.config.progressLogIntervalMs;
  const abortMonitor = createAbortMonitor(context, runtime);
  const progressLogger = createProgressLogger(runtime, profileStart, progressLogIntervalMs);

  try {
    abortMonitor.start();
    progressLogger.start();

    while (!runtime.shouldAbort && Date.now() - profileStart < totalDurationMs) {
      const elapsedMs = Date.now() - profileStart;
      const targetVus = computeTargetVus(runtime.profileConfig, elapsedMs);
      reconcileWorkers(context, runtime, targetVus);
      await sleep(schedulerTickMs);
    }
  } finally {
    runtime.shouldAbort = true;
    reconcileWorkers(context, runtime, 0);
    await waitForWorkersToStop(runtime);
    abortMonitor.stop();
    progressLogger.stop();
    context.runtime = null;
  }

  const endedAt = Date.now();
  const dockerAfter = captureDockerStats(context.config);
  const overall = summarizeMetric(runtime.metrics.overall, endedAt - profileStart);
  const perEndpoint = [...runtime.metrics.byEndpoint.values()]
    .map((metric) => summarizeMetric(metric, endedAt - profileStart))
    .sort((left, right) => right.p95Ms - left.p95Ms);

  return {
    profile: profileName,
    startedAt: new Date(profileStart).toISOString(),
    endedAt: new Date(endedAt).toISOString(),
    durationMs: endedAt - profileStart,
    configuredDurationMs: totalDurationMs,
    aborted: Boolean(runtime.abortedReason),
    abortedReason: runtime.abortedReason,
    preflight,
    phases: buildPhaseSummary(runtime.profileConfig),
    scenarioIterations: runtime.scenarioIterations,
    overall,
    perEndpoint,
    slowestEndpoints: perEndpoint.slice(0, 3),
    docker: {
      before: dockerBefore,
      after: dockerAfter
    }
  };
}

function getProfileDurationMs(profileConfig) {
  return profileConfig.warmup.durationMs
    + profileConfig.ramp.durationMs
    + profileConfig.sustain.durationMs
    + profileConfig.cooldown.durationMs;
}

function buildPhaseSummary(profileConfig) {
  return {
    warmup: profileConfig.warmup,
    ramp: profileConfig.ramp,
    sustain: profileConfig.sustain,
    cooldown: profileConfig.cooldown,
    abort: profileConfig.abort ?? null
  };
}

function computeTargetVus(profileConfig, elapsedMs) {
  const warmupEnd = profileConfig.warmup.durationMs;
  const rampEnd = warmupEnd + profileConfig.ramp.durationMs;
  const sustainEnd = rampEnd + profileConfig.sustain.durationMs;
  const cooldownEnd = sustainEnd + profileConfig.cooldown.durationMs;

  if (elapsedMs < warmupEnd) {
    return profileConfig.warmup.targetVus;
  }

  if (elapsedMs < rampEnd) {
    const phaseProgress = (elapsedMs - warmupEnd) / profileConfig.ramp.durationMs;
    return Math.round(lerp(profileConfig.warmup.targetVus, profileConfig.ramp.targetVus, phaseProgress));
  }

  if (elapsedMs < sustainEnd) {
    return profileConfig.sustain.targetVus;
  }

  if (elapsedMs < cooldownEnd) {
    const phaseProgress = (elapsedMs - sustainEnd) / profileConfig.cooldown.durationMs;
    return Math.max(0, Math.round(lerp(profileConfig.sustain.targetVus, profileConfig.cooldown.targetVus, phaseProgress)));
  }

  return 0;
}

function lerp(start, end, progress) {
  return start + (end - start) * progress;
}

function reconcileWorkers(context, runtime, targetVus) {
  const currentWorkers = [...runtime.workers.values()];

  if (currentWorkers.length < targetVus) {
    const missingWorkers = targetVus - currentWorkers.length;
    for (let index = 0; index < missingWorkers; index += 1) {
      startWorker(context, runtime);
    }
    return;
  }

  if (currentWorkers.length > targetVus) {
    const excessWorkers = currentWorkers.length - targetVus;
    const candidates = currentWorkers.filter((worker) => !worker.shouldStop).sort((left, right) => right.id - left.id);
    for (let index = 0; index < excessWorkers && index < candidates.length; index += 1) {
      candidates[index].shouldStop = true;
    }
  }
}

function startWorker(context, runtime) {
  const worker = {
    id: runtime.nextWorkerId,
    shouldStop: false,
    promise: null
  };
  runtime.nextWorkerId += 1;
  runtime.workers.set(worker.id, worker);
  worker.promise = workerLoop(context, runtime, worker)
    .catch((error) => {
      runtime.abortedReason = runtime.abortedReason ?? `Worker ${worker.id} crashed: ${error.message}`;
      runtime.shouldAbort = true;
    })
    .finally(() => {
      runtime.workers.delete(worker.id);
    });
}

async function waitForWorkersToStop(runtime) {
  const workers = [...runtime.workers.values()].map((worker) => worker.promise).filter(Boolean);
  await Promise.allSettled(workers);
}

async function workerLoop(context, runtime, worker) {
  while (!worker.shouldStop && !runtime.shouldAbort) {
    const scenarioName = selectScenario(runtime, context.config.mix);
    runtime.scenarioIterations[scenarioName] += 1;

    if (scenarioName === "publicBrowse") {
      await runPublicBrowseScenario(context, runtime.preflight);
    } else {
      await runAdminReadScenario(context, runtime.preflight);
    }

    if (worker.shouldStop || runtime.shouldAbort) {
      break;
    }

    await sleep(randomBetween(context.config.thinkTimeMs.min, context.config.thinkTimeMs.max));
  }
}

function selectScenario(runtime, mix) {
  const totalIterations = runtime.scenarioIterations.publicBrowse + runtime.scenarioIterations.adminRead;
  const targetPublicRatio = mix.publicBrowse / 100;

  if (totalIterations === 0) {
    return "publicBrowse";
  }

  const currentPublicRatio = runtime.scenarioIterations.publicBrowse / totalIterations;
  if (currentPublicRatio < targetPublicRatio) {
    return "publicBrowse";
  }

  return "adminRead";
}

async function runPublicBrowseScenario(context, preflight) {
  await performRequest(context, "/api/categories/public/tree", {
    metricKey: "GET /api/categories/public/tree",
    measure: true
  });
  await performRequest(context, "/api/services/public/all", {
    metricKey: "GET /api/services/public/all",
    measure: true
  });
  await performRequest(context, `/api/services/public/${preflight.fixtures.serviceId}`, {
    metricKey: "GET /api/services/public/{serviceId}",
    measure: true
  });
  await performRequest(context, `/api/freelancer/${preflight.fixtures.freelancerId}/profile`, {
    metricKey: "GET /api/freelancer/{freelancerId}/profile",
    measure: true
  });
  await performRequest(context, `/api/services/public/freelancer/${preflight.fixtures.freelancerId}`, {
    metricKey: "GET /api/services/public/freelancer/{freelancerId}",
    measure: true
  });
}

async function runAdminReadScenario(context, preflight) {
  await performAdminRequest(context, "/api/admin/dashboard", {
    metricKey: "GET /api/admin/dashboard",
    measure: true
  });
  await performAdminRequest(context, "/api/admin/users?page=0&size=20", {
    metricKey: "GET /api/admin/users?page=0&size=20",
    measure: true
  });
  await performAdminRequest(context, `/api/admin/users/${preflight.fixtures.adminUserId}`, {
    metricKey: "GET /api/admin/users/{id}",
    measure: true
  });
}

function createAbortMonitor(context, runtime) {
  let timer = null;
  let consecutiveWindowMs = 0;
  const threshold = runtime.profileConfig.abort?.errorRateThreshold;
  const requiredWindowMs = runtime.profileConfig.abort?.windowMs;

  return {
    start() {
      if (threshold === undefined || requiredWindowMs === undefined) {
        return;
      }

      timer = setInterval(() => {
        const requests = runtime.recentWindow.requests;
        const errors = runtime.recentWindow.errors;
        runtime.recentWindow.requests = 0;
        runtime.recentWindow.errors = 0;

        if (requests === 0) {
          consecutiveWindowMs = 0;
          return;
        }

        const errorRate = errors / requests;
        if (errorRate > threshold) {
          consecutiveWindowMs += context.config.abortCheckIntervalMs;
          if (consecutiveWindowMs >= requiredWindowMs) {
            runtime.abortedReason = `Abort threshold reached: ${(errorRate * 100).toFixed(2)}% errors for ${Math.round(consecutiveWindowMs / 1000)}s.`;
            runtime.shouldAbort = true;
          }
        } else {
          consecutiveWindowMs = 0;
        }
      }, context.config.abortCheckIntervalMs);
    },
    stop() {
      if (timer) {
        clearInterval(timer);
        timer = null;
      }
    }
  };
}

function createProgressLogger(runtime, startedAt, intervalMs) {
  let timer = null;

  return {
    start() {
      timer = setInterval(() => {
        const elapsedMs = Date.now() - startedAt;
        const overall = summarizeMetric(runtime.metrics.overall, elapsedMs);
        console.log(
          `[${runtime.profileName}] elapsed=${Math.round(elapsedMs / 1000)}s activeWorkers=${runtime.workers.size} requests=${overall.requestCount} req/s=${overall.requestsPerSecond.toFixed(2)} errorRate=${(overall.errorRate * 100).toFixed(2)}%`
        );
      }, intervalMs);
    },
    stop() {
      if (timer) {
        clearInterval(timer);
        timer = null;
      }
    }
  };
}

function randomBetween(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function captureDockerStats(config) {
  if (!config.docker?.enabled) {
    return {
      available: false,
      reason: "disabled in config"
    };
  }

  const containers = config.docker.containers ?? [];
  if (containers.length === 0) {
    return {
      available: false,
      reason: "no containers configured"
    };
  }

  const result = spawnSync(
    "docker",
    ["stats", "--no-stream", "--format", "{{json .}}", ...containers],
    {
      cwd: ROOT_DIR,
      encoding: "utf8",
      timeout: 15000
    }
  );

  if (result.error) {
    return {
      available: false,
      reason: result.error.message
    };
  }

  if (result.status !== 0) {
    return {
      available: false,
      reason: (result.stderr || result.stdout || `docker stats exited with code ${result.status}`).trim()
    };
  }

  const lines = result.stdout
    .split(/\r?\n/u)
    .map((line) => line.trim())
    .filter(Boolean);

  const snapshots = [];
  for (const line of lines) {
    try {
      snapshots.push(JSON.parse(line));
    } catch (error) {
      return {
        available: false,
        reason: `Could not parse docker stats output: ${error.message}`
      };
    }
  }

  return {
    available: true,
    capturedAt: new Date().toISOString(),
    snapshots
  };
}

function buildComparison(results) {
  return results.map((result) => ({
    profile: result.profile,
    durationMs: result.durationMs,
    aborted: result.aborted,
    requests: result.overall.requestCount,
    requestsPerSecond: result.overall.requestsPerSecond,
    errorRate: result.overall.errorRate,
    p95Ms: result.overall.p95Ms,
    p99Ms: result.overall.p99Ms,
    maxMs: result.overall.maxMs
  }));
}

function renderPreflightMarkdown(payload) {
  const lines = [
    "# Load Test Preflight",
    "",
    `Generated at: ${payload.generatedAt}`,
    `Base URL: ${payload.baseUrl}`,
    ""
  ];

  for (const result of payload.results) {
    lines.push(`## Profile ${result.profile}`);
    lines.push("");
    lines.push(`- Duration: ${formatDuration(result.preflight.durationMs)}`);
    lines.push(`- Fixture service: ${result.preflight.fixtures.serviceId}`);
    lines.push(`- Fixture freelancer: ${result.preflight.fixtures.freelancerId}`);
    lines.push(`- Fixture admin user: ${result.preflight.fixtures.adminUserId}`);
    lines.push("");
    lines.push("| Check | Status | Duration |");
    lines.push("| --- | --- | ---: |");
    for (const check of result.preflight.checks) {
      lines.push(`| ${check.name} | ${check.status} | ${formatMs(check.durationMs)} |`);
    }
    lines.push("");
  }

  return lines.join("\n");
}

function renderSummaryMarkdown(summary) {
  const lines = [
    "# Local API Load Test Summary",
    "",
    `Generated at: ${summary.generatedAt}`,
    `Base URL: ${summary.baseUrl}`,
    ""
  ];

  lines.push("## Comparison");
  lines.push("");
  lines.push("| Profile | Duration | Aborted | Requests | Req/s | Error Rate | p95 | p99 | Max |");
  lines.push("| --- | ---: | --- | ---: | ---: | ---: | ---: | ---: | ---: |");
  for (const row of summary.comparison) {
    lines.push(
      `| ${row.profile} | ${formatDuration(row.durationMs)} | ${row.aborted ? "yes" : "no"} | ${formatInteger(row.requests)} | ${formatDecimal(row.requestsPerSecond)} | ${formatPercent(row.errorRate)} | ${formatMs(row.p95Ms)} | ${formatMs(row.p99Ms)} | ${formatMs(row.maxMs)} |`
    );
  }
  lines.push("");

  for (const result of summary.results) {
    lines.push(`## Profile ${result.profile}`);
    lines.push("");
    lines.push(`- Started at: ${result.startedAt}`);
    lines.push(`- Ended at: ${result.endedAt}`);
    lines.push(`- Duration: ${formatDuration(result.durationMs)}`);
    lines.push(`- Aborted: ${result.aborted ? `yes (${result.abortedReason})` : "no"}`);
    lines.push(`- Scenario iterations: public=${formatInteger(result.scenarioIterations.publicBrowse)}, admin=${formatInteger(result.scenarioIterations.adminRead)}`);
    lines.push(`- Fixtures: service=${result.preflight.fixtures.serviceId}, freelancer=${result.preflight.fixtures.freelancerId}, adminUser=${result.preflight.fixtures.adminUserId}`);
    lines.push("");
    lines.push("### Overall");
    lines.push("");
    lines.push("| Requests | Req/s | Error Rate | Avg | p50 | p95 | p99 | Max |");
    lines.push("| ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |");
    lines.push(`| ${formatInteger(result.overall.requestCount)} | ${formatDecimal(result.overall.requestsPerSecond)} | ${formatPercent(result.overall.errorRate)} | ${formatMs(result.overall.averageMs)} | ${formatMs(result.overall.p50Ms)} | ${formatMs(result.overall.p95Ms)} | ${formatMs(result.overall.p99Ms)} | ${formatMs(result.overall.maxMs)} |`);
    lines.push("");
    lines.push(`Status distribution: ${formatStatusMap(result.overall.statuses)}`);
    lines.push("");
    lines.push("### Slowest Endpoints");
    lines.push("");
    lines.push("| Endpoint | Requests | Error Rate | Avg | p95 | p99 | Max |");
    lines.push("| --- | ---: | ---: | ---: | ---: | ---: | ---: |");
    for (const endpoint of result.slowestEndpoints) {
      lines.push(`| ${endpoint.name} | ${formatInteger(endpoint.requestCount)} | ${formatPercent(endpoint.errorRate)} | ${formatMs(endpoint.averageMs)} | ${formatMs(endpoint.p95Ms)} | ${formatMs(endpoint.p99Ms)} | ${formatMs(endpoint.maxMs)} |`);
    }
    lines.push("");
    lines.push("### All Endpoints");
    lines.push("");
    lines.push("| Endpoint | Requests | Req/s | Error Rate | Avg | p50 | p95 | p99 | Max |");
    lines.push("| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |");
    for (const endpoint of result.perEndpoint) {
      lines.push(`| ${endpoint.name} | ${formatInteger(endpoint.requestCount)} | ${formatDecimal(endpoint.requestsPerSecond)} | ${formatPercent(endpoint.errorRate)} | ${formatMs(endpoint.averageMs)} | ${formatMs(endpoint.p50Ms)} | ${formatMs(endpoint.p95Ms)} | ${formatMs(endpoint.p99Ms)} | ${formatMs(endpoint.maxMs)} |`);
    }
    lines.push("");
    lines.push("### Docker Snapshots");
    lines.push("");
    lines.push(`- Before: ${formatDockerSnapshot(result.docker.before)}`);
    lines.push(`- After: ${formatDockerSnapshot(result.docker.after)}`);
    lines.push("");
  }

  return lines.join("\n");
}

function formatDuration(durationMs) {
  if (durationMs === null || durationMs === undefined) {
    return "n/a";
  }

  const totalSeconds = Math.round(durationMs / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}m ${seconds}s`;
}

function formatMs(value) {
  if (value === null || value === undefined) {
    return "n/a";
  }

  return `${Number(value).toFixed(2)} ms`;
}

function formatDecimal(value) {
  return Number(value ?? 0).toFixed(2);
}

function formatPercent(value) {
  return `${(Number(value ?? 0) * 100).toFixed(2)}%`;
}

function formatInteger(value) {
  return new Intl.NumberFormat("en-US").format(Number(value ?? 0));
}

function formatStatusMap(statuses) {
  return Object.entries(statuses)
    .map(([status, count]) => `${status}: ${count}`)
    .join(", ");
}

function formatDockerSnapshot(snapshot) {
  if (!snapshot?.available) {
    return `unavailable (${snapshot?.reason ?? "unknown reason"})`;
  }

  return snapshot.snapshots
    .map((item) => `${item.Name}: CPU ${item.CPUPerc}, Mem ${item.MemUsage}`)
    .join(" | ");
}

async function writeJson(targetPath, payload) {
  await writeText(targetPath, JSON.stringify(payload, null, 2));
}

async function writeText(targetPath, contents) {
  await fsp.mkdir(path.dirname(targetPath), { recursive: true });
  await fsp.writeFile(targetPath, contents, "utf8");
}

main().catch((error) => {
  console.error(error.message);
  process.exitCode = 1;
});
