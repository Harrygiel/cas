const puppeteer = require("puppeteer");
const assert = require("assert");
const cas = require("../../cas.js");

(async () => {
    const browser = await puppeteer.launch(cas.browserOptions());
    const page = await cas.newPage(browser);
    
    await cas.gotoLogin(page);
    await cas.waitForTimeout(page);

    const pswd = await page.$("#password");
    assert(pswd === null);

    await cas.type(page,"#username", "user3+casuser");
    await cas.pressEnter(page);
    await cas.waitForTimeout(page);
    await cas.assertInnerText(page, "#login h3", "Provide Token");
    await cas.assertInnerTextStartsWith(page, "#login p", "Please provide the security token sent to you");
    await cas.assertVisibility(page, "#token");
    await cas.waitForTimeout(page);

    const code = await cas.extractFromEmailMessage(browser);

    await page.bringToFront();
    await cas.type(page, "#token", code);
    await cas.submitForm(page, "#fm1");
    await cas.waitForTimeout(page);

    await cas.assertCookie(page);
    await cas.assertInnerTextStartsWith(page, "#content div p", "You, user3, have successfully logged in");

    await cas.click(page, "#auth-tab");
    await cas.waitForTimeout(page);
    await cas.type(page, "#attribute-tab-1 input[type=search]", "surrogate");
    await cas.waitForTimeout(page);
    await cas.screenshot(page);
    
    await cas.assertInnerTextStartsWith(page, "#surrogateEnabled td code kbd", "[true]");
    await cas.assertInnerTextStartsWith(page, "#surrogatePrincipal td code kbd", "[casuser]");
    await cas.assertInnerTextStartsWith(page, "#surrogateUser td code kbd", "[user3]");
    await cas.waitForTimeout(page);

    await browser.close();
})();
