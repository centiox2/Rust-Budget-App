import { test, expect } from "@playwright/test";
import { demoData } from "../src/model.mjs";
const screenshotDir = process.env.CI ? "screenshots" : "../../outputs";
test("onboarding, CRUD, budgets, analytics, persistence and backup", async ({
  page,
}) => {
  const errors: string[] = [];
  page.on("pageerror", (e) => errors.push(e.message));
  await page.goto("/");
  await expect(
    page.getByRole("heading", { name: /Maintain Your/ }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Start Now" }).click();
  await page.getByLabel("Your name").fill("Alex");
  await page.getByRole("button", { name: "Let’s get started" }).click();
  await expect(page.getByRole("heading", { name: "Alex" })).toBeVisible();
  await page
    .getByRole("button", { name: "Add transaction", exact: true })
    .first()
    .click();
  let dialog = page.getByRole("dialog");
  await dialog.getByRole("button", { name: "Income", exact: true }).click();
  await dialog.getByLabel("Amount", { exact: true }).fill("2000");
  await dialog.getByLabel("What was it for?").fill("Salary");
  await dialog
    .getByRole("button", { name: "Add transaction", exact: true })
    .click();
  await expect(page.locator(".balance-card h2")).toHaveText("$2,000.00");
  await page
    .getByRole("button", { name: "Add transaction", exact: true })
    .first()
    .click();
  dialog = page.getByRole("dialog");
  await dialog.getByLabel("Amount", { exact: true }).fill("49.99");
  await dialog.getByLabel("What was it for?").fill("Market basket");
  await dialog
    .getByLabel("Category", { exact: true })
    .selectOption("Groceries");
  await dialog
    .getByRole("button", { name: "Add transaction", exact: true })
    .click();
  await expect(page.locator(".balance-card h2")).toHaveText("$1,950.01");
  await page.getByRole("button", { name: "Set a budget", exact: true }).click();
  await page.getByLabel("Groceries budget").fill("100");
  await page.getByRole("button", { name: "Save budgets" }).click();
  await page
    .getByRole("navigation")
    .getByRole("button", { name: "Budgets", exact: true })
    .click();
  await expect(page.locator(".budget-card")).toContainText(
    "$50.01 left to spend",
  );
  await page
    .getByRole("navigation")
    .getByRole("button", { name: "Analytics", exact: true })
    .click();
  await expect(page.locator(".chart-total")).toHaveText("$49.99");
  await page
    .getByRole("navigation")
    .getByRole("button", { name: "Transactions", exact: true })
    .click();
  await page.getByRole("button", { name: /Market basket/ }).click();
  await page.getByLabel("Amount", { exact: true }).fill("59.99");
  await page.getByRole("button", { name: "Save changes" }).click();
  await page.reload();
  await expect(page.locator(".balance-card h2")).toHaveText("$1,940.01");
  await page.getByRole("button", { name: /Market basket/ }).click();
  await page.getByRole("button", { name: "Delete transaction" }).click();
  await page.getByRole("button", { name: "Delete", exact: true }).click();
  await expect(page.locator(".balance-card h2")).toHaveText("$2,000.00");
  await page
    .getByRole("navigation")
    .getByRole("button", { name: "Settings", exact: true })
    .click();
  const downloadPromise = page.waitForEvent("download");
  await page.getByRole("button", { name: "Export backup" }).click();
  const download = await downloadPromise;
  expect(download.suggestedFilename()).toContain("verdant");
  expect(errors).toEqual([]);
});
test("sample app looks good at mobile sizes", async ({ page }) => {
  await page.goto("/");
  await page.screenshot({ path: `${screenshotDir}/verdant-welcome.png` });
  await page.getByRole("button", { name: "Start Now" }).click();
  await page.getByLabel("Your name").fill("Alex");
  await page.getByLabel("Explore with sample data").check();
  await page.getByRole("button", { name: "Let’s get started" }).click();
  for (const width of [360, 393, 430, 768]) {
    await page.setViewportSize({ width, height: 900 });
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
    ).toBeTruthy();
  }
  await page.setViewportSize({ width: 393, height: 900 });
  await page.screenshot({
    path: `${screenshotDir}/verdant-home.png`,
    fullPage: false,
  });
  await page
    .getByRole("navigation")
    .getByRole("button", { name: "Analytics", exact: true })
    .click();
  await page.screenshot({
    path: `${screenshotDir}/verdant-analytics.png`,
    fullPage: false,
  });
});

test("native storage bridge, invalid input, backup recovery and save failures", async ({
  page,
}) => {
  const initial = demoData("Taylor", "KES");
  await page.addInitScript((data) => {
    (window as any).isTauri = true;
    (window as any).__nativeData =
      JSON.parse(sessionStorage.getItem("native") || "null") || data;
    (window as any).__TAURI_INTERNALS__ = {
      invoke: async (command: string, args: any) => {
        if (command === "load_data") return (window as any).__nativeData;
        if (command === "save_data") {
          if ((window as any).__failSave) throw Error("Disk full");
          (window as any).__nativeData = args.data;
          sessionStorage.setItem("native", JSON.stringify(args.data));
          return;
        }
        throw Error("Unexpected command " + command);
      },
    };
  }, initial);
  await page.goto("/");
  await expect(page.getByRole("heading", { name: "Taylor" })).toBeVisible();
  const before = await page.locator(".balance-card h2").textContent();
  await page.getByRole("button", { name: "Income", exact: true }).click();
  await page.getByLabel("Amount", { exact: true }).fill("2.999");
  await page.getByLabel("What was it for?").fill("Extra income");
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Add transaction" })
    .click();
  await expect(page.getByRole("alert")).toContainText("2 decimals");
  await page.getByLabel("Amount", { exact: true }).fill("10");
  await page.evaluate(() => ((window as any).__failSave = true));
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Add transaction" })
    .click();
  await expect(page.getByRole("alert")).toContainText("Disk full");
  await expect(page.locator(".balance-card h2")).toHaveText(before!);
  await page.evaluate(() => ((window as any).__failSave = false));
  await page.getByRole("button", { name: "Dismiss error" }).click();
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Add transaction" })
    .click();
  await page.reload();
  await expect(page.locator(".balance-card h2")).not.toHaveText(before!);
  await page
    .getByRole("navigation")
    .getByRole("button", { name: "Settings", exact: true })
    .click();
  await page.getByRole("button", { name: "Export backup" }).click();
  const backup = await page
    .getByRole("dialog")
    .getByRole("textbox")
    .inputValue();
  expect(JSON.parse(backup).transactions.length).toBe(
    initial.transactions.length + 1,
  );
  await page.getByRole("button", { name: "Close", exact: true }).click();
  await page
    .getByRole("button", { name: "Restore backup", exact: true })
    .click();
  await page.getByPlaceholder("Paste backup JSON").fill('{"version":99}');
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Restore backup" })
    .click();
  await expect(page.getByRole("alert")).toContainText("valid Verdant backup");
  await page.getByRole("button", { name: "Dismiss error" }).click();
  await page
    .getByPlaceholder("Paste backup JSON")
    .fill(JSON.stringify(initial));
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Restore backup" })
    .click();
  await page
    .getByRole("navigation")
    .getByRole("button", { name: "Home", exact: true })
    .click();
  await expect(page.locator(".balance-card h2")).toHaveText(before!);
});
