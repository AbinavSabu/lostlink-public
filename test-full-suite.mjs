import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';

const BASE_URL = 'http://localhost:8081';
const FRONTEND_URL = 'http://localhost:5173';

const results = [];

function logSection(title) {
    console.log(`\n==================================================`);
    console.log(title);
    console.log(`==================================================`);
}

function recordResult(category, testName, status, details = {}) {
    const entry = { category, testName, status, details };
    results.push(entry);
    const symbol = status === 'PASS' ? '✅ [PASS]' : (status === 'FAIL' ? '❌ [FAIL]' : '⚠️ [PARTIAL]');
    console.log(`${symbol} ${category} - ${testName}`);
    if (Object.keys(details).length > 0) {
        console.log(`   Details:`, JSON.stringify(details));
    }
}

async function request(endpoint, options = {}) {
    const url = endpoint.startsWith('http') ? endpoint : `${BASE_URL}${endpoint}`;
    const headers = { ...options.headers };
    
    if (options.token) {
        headers['Authorization'] = `Bearer ${options.token}`;
    }
    
    let body = options.body;
    if (body && typeof body === 'object' && !(body instanceof FormData) && !(body instanceof Buffer)) {
        headers['Content-Type'] = 'application/json';
        body = JSON.stringify(body);
    }

    try {
        const res = await fetch(url, {
            method: options.method || 'GET',
            headers,
            body
        });
        
        let data = null;
        const contentType = res.headers.get('content-type') || '';
        if (contentType.includes('application/json')) {
            try { data = await res.json(); } catch (_) { data = null; }
        } else if (contentType.includes('text/') || contentType.includes('application/csv')) {
            data = await res.text();
        } else {
            const buf = await res.arrayBuffer();
            data = Buffer.from(buf);
        }

        return {
            status: res.status,
            ok: res.ok,
            headers: res.headers,
            data
        };
    } catch (err) {
        return {
            status: 0,
            ok: false,
            error: err.message,
            data: null
        };
    }
}

async function runAllTests() {
    console.log(`Starting Campus Lost & Found Comprehensive E2E Verification...`);

    const runId = Math.random().toString(36).substring(2, 7);
    const userA_email = `alice_${runId}@campus.edu`;
    const userA_pass = `AlicePass@123`;
    let userA_token = '';
    let userA_id = null;

    const userB_email = `bob_${runId}@campus.edu`;
    const userB_pass = `BobPass@123`;
    let userB_token = '';
    let userB_id = null;

    const userC_email = `charlie_${runId}@campus.edu`;
    const userC_pass = `CharliePass@123`;
    let userC_token = '';
    let userC_id = null;

    const admin_email = 'admin_e2e@lostfound.edu';
    const admin_pass = 'Admin@123';
    let admin_token = '';
    let admin_id = null;

    // ----------------------------------------------------
    // 1. ENVIRONMENT / STARTUP TEST
    // ----------------------------------------------------
    logSection('1. ENVIRONMENT / STARTUP TEST');
    const beHealth = await request('/api/items');
    if (beHealth.status === 200) {
        recordResult('Environment', 'Backend Running on Port 8081', 'PASS', { status: beHealth.status });
    } else {
        recordResult('Environment', 'Backend Running on Port 8081', 'FAIL', { status: beHealth.status, error: beHealth.error });
    }

    const feHealth = await request(FRONTEND_URL);
    if (feHealth.status === 200) {
        recordResult('Environment', 'Frontend Running on Port 5173', 'PASS', { status: feHealth.status });
    } else {
        recordResult('Environment', 'Frontend Running on Port 5173', 'FAIL', { status: feHealth.status, error: feHealth.error });
    }

    const wsHealth = await request('/ws/info');
    if (wsHealth.status === 200) {
        recordResult('Environment', 'WebSocket SockJS Endpoint Active', 'PASS', { status: wsHealth.status });
    } else {
        recordResult('Environment', 'WebSocket SockJS Endpoint Active', 'FAIL', { status: wsHealth.status });
    }

    const h2Health = await request('/h2-console');
    if (h2Health.status === 200) {
        recordResult('Environment', 'Embedded H2 Console Accessible', 'PASS', { status: h2Health.status });
    } else {
        recordResult('Environment', 'Embedded H2 Console Accessible', 'FAIL', { status: h2Health.status });
    }

    // ----------------------------------------------------
    // 2. AUTHENTICATION TEST
    // ----------------------------------------------------
    logSection('2. AUTHENTICATION TEST');

    // Register User A
    const regA = await request('/api/auth/register', {
        method: 'POST',
        body: { name: 'Alice Student', email: userA_email, password: userA_pass }
    });
    if (regA.status === 201 && regA.data?.token) {
        userA_token = regA.data.token;
        userA_id = regA.data.id;
        recordResult('Auth', 'Student Registration (User A)', 'PASS', { id: userA_id, email: userA_email, role: regA.data.role });
    } else {
        recordResult('Auth', 'Student Registration (User A)', 'FAIL', { status: regA.status, data: regA.data });
    }

    // Duplicate Registration
    const dupReg = await request('/api/auth/register', {
        method: 'POST',
        body: { name: 'Alice Duplicate', email: userA_email, password: userA_pass }
    });
    if (dupReg.status === 400) {
        recordResult('Auth', 'Duplicate Email Registration Rejection', 'PASS', { status: 400 });
    } else {
        recordResult('Auth', 'Duplicate Email Registration Rejection', 'FAIL', { status: dupReg.status });
    }

    // Login User A
    const loginA = await request('/api/auth/login', {
        method: 'POST',
        body: { email: userA_email, password: userA_pass }
    });
    if (loginA.status === 200 && loginA.data?.token) {
        userA_token = loginA.data.token;
        recordResult('Auth', 'Student Login with Valid Credentials', 'PASS', { status: 200 });
    } else {
        recordResult('Auth', 'Student Login with Valid Credentials', 'FAIL', { status: loginA.status });
    }

    // Login with Wrong Password
    const wrongPass = await request('/api/auth/login', {
        method: 'POST',
        body: { email: userA_email, password: 'WrongPassword999' }
    });
    if (wrongPass.status === 400 || wrongPass.status === 401) {
        recordResult('Auth', 'Login Rejection with Wrong Password', 'PASS', { status: wrongPass.status });
    } else {
        recordResult('Auth', 'Login Rejection with Wrong Password', 'FAIL', { status: wrongPass.status });
    }

    // Login with Nonexistent Account
    const nonExistent = await request('/api/auth/login', {
        method: 'POST',
        body: { email: 'nonexistent_user_999@campus.edu', password: 'SomePassword@123' }
    });
    if (nonExistent.status === 400 || nonExistent.status === 401) {
        recordResult('Auth', 'Login Rejection with Nonexistent Account', 'PASS', { status: nonExistent.status });
    } else {
        recordResult('Auth', 'Login Rejection with Nonexistent Account', 'FAIL', { status: nonExistent.status });
    }

    // Register User B and User C
    const regB = await request('/api/auth/register', {
        method: 'POST',
        body: { name: 'Bob Finder', email: userB_email, password: userB_pass }
    });
    if (regB.status === 201) {
        userB_token = regB.data.token;
        userB_id = regB.data.id;
    }

    const regC = await request('/api/auth/register', {
        method: 'POST',
        body: { name: 'Charlie Intruder', email: userC_email, password: userC_pass }
    });
    if (regC.status === 201) {
        userC_token = regC.data.token;
        userC_id = regC.data.id;
    }

    // Unauthenticated Access to Protected Route
    const unauthAccess = await request('/api/items/my-items');
    if (unauthAccess.status === 401) {
        recordResult('Auth', 'Protected Route Rejects Unauthenticated Request', 'PASS', { status: 401 });
    } else {
        recordResult('Auth', 'Protected Route Rejects Unauthenticated Request', 'FAIL', { status: unauthAccess.status });
    }

    // Admin Login
    const adminLogin = await request('/api/auth/login', {
        method: 'POST',
        body: { email: admin_email, password: admin_pass }
    });
    if (adminLogin.status === 200 && adminLogin.data?.token) {
        admin_token = adminLogin.data.token;
        admin_id = adminLogin.data.id;
        recordResult('Auth', 'Admin Login Successful', 'PASS', { role: adminLogin.data.role, id: admin_id });
    } else {
        recordResult('Auth', 'Admin Login Successful', 'FAIL', { status: adminLogin.status });
    }

    // Non-Admin Calling Admin Route
    const studentToAdmin = await request('/api/admin/stats', { token: userA_token });
    if (studentToAdmin.status === 403) {
        recordResult('Auth', 'Student Forbidden from Accessing /api/admin/stats', 'PASS', { status: 403 });
    } else {
        recordResult('Auth', 'Student Forbidden from Accessing /api/admin/stats', 'FAIL', { status: studentToAdmin.status });
    }

    // Admin Calling Admin Route
    const adminAccess = await request('/api/admin/stats', { token: admin_token });
    if (adminAccess.status === 200) {
        recordResult('Auth', 'Admin Access to /api/admin/stats Allowed', 'PASS', { stats: adminAccess.data });
    } else {
        recordResult('Auth', 'Admin Access to /api/admin/stats Allowed', 'FAIL', { status: adminAccess.status });
    }

    // ----------------------------------------------------
    // 3. USER PROFILE TEST
    // ----------------------------------------------------
    logSection('3. USER PROFILE TEST');
    const meRes = await request('/api/auth/me', { token: userA_token });
    if (meRes.status === 200 && meRes.data?.email === userA_email) {
        recordResult('Profile', 'View Current User Profile', 'PASS', meRes.data);
    } else {
        recordResult('Profile', 'View Current User Profile', 'FAIL', { status: meRes.status, data: meRes.data });
    }

    // Update Profile
    const updateProf = await request('/api/auth/profile', {
        method: 'PUT',
        token: userA_token,
        body: { name: 'Alice Updated', phoneNumber: '+1-555-0199' }
    });
    if (updateProf.status === 200 && updateProf.data?.name === 'Alice Updated' && updateProf.data?.phoneNumber === '+1-555-0199') {
        recordResult('Profile', 'Update Profile Name and Phone', 'PASS', updateProf.data);
    } else {
        recordResult('Profile', 'Update Profile Name and Phone', 'FAIL', { status: updateProf.status, data: updateProf.data });
    }

    // Verify Profile Persistence
    const meVerify = await request('/api/auth/me', { token: userA_token });
    if (meVerify.status === 200 && meVerify.data?.name === 'Alice Updated') {
        recordResult('Profile', 'Profile Update Persists on Reload', 'PASS', meVerify.data);
    } else {
        recordResult('Profile', 'Profile Update Persists on Reload', 'FAIL', { status: meVerify.status, data: meVerify.data });
    }

    // Change Password
    const newPass = 'AliceNewPass@456';
    const changePassRes = await request('/api/auth/change-password', {
        method: 'POST',
        token: userA_token,
        body: { oldPassword: userA_pass, newPassword: newPass }
    });
    if (changePassRes.status === 200) {
        // Test login with old password fails
        const testOld = await request('/api/auth/login', {
            method: 'POST',
            body: { email: userA_email, password: userA_pass }
        });
        // Test login with new password succeeds
        const testNew = await request('/api/auth/login', {
            method: 'POST',
            body: { email: userA_email, password: newPass }
        });
        if (testOld.status !== 200 && testNew.status === 200) {
            userA_token = testNew.data.token;
            recordResult('Profile', 'Change Password Verified', 'PASS', { oldPassRejected: testOld.status, newPassAccepted: testNew.status });
        } else {
            recordResult('Profile', 'Change Password Verified', 'FAIL', { testOld: testOld.status, testNew: testNew.status });
        }
    } else {
        recordResult('Profile', 'Change Password', 'FAIL', { status: changePassRes.status, data: changePassRes.data });
    }

    // ----------------------------------------------------
    // 4. LOST ITEM FUNCTIONALITY
    // ----------------------------------------------------
    logSection('4. LOST ITEM FUNCTIONALITY');

    let lostItemA = null;
    const createLostRes = await request('/api/items', {
        method: 'POST',
        token: userA_token,
        body: {
            title: `Lost Blue Backpack ${runId}`,
            description: 'Navy blue JanSport with laptop inside',
            category: 'BAGS',
            location: 'Science Library 2nd Floor',
            status: 'LOST',
            date: '2026-10-04',
            reward: '$20',
            verificationQuestion: 'What keychain is attached?'
        }
    });

    if (createLostRes.status === 201 && createLostRes.data?.id) {
        lostItemA = createLostRes.data;
        recordResult('Lost Item', 'Create LOST Item Report', 'PASS', { id: lostItemA.id, title: lostItemA.title, status: lostItemA.status });
    } else {
        recordResult('Lost Item', 'Create LOST Item Report', 'FAIL', { status: createLostRes.status, data: createLostRes.data });
    }

    // Verify in my-items
    const myItemsA = await request('/api/items/my-items', { token: userA_token });
    const foundInMyItems = myItemsA.data?.some?.(i => i.id === lostItemA?.id);
    if (foundInMyItems) {
        recordResult('Lost Item', 'Item Appears in MyItems Feed', 'PASS', { count: myItemsA.data.length });
    } else {
        recordResult('Lost Item', 'Item Appears in MyItems Feed', 'FAIL', { count: myItemsA.data?.length });
    }

    // Edit Item
    const editItemRes = await request(`/api/items/${lostItemA.id}`, {
        method: 'PUT',
        token: userA_token,
        body: {
            title: `Lost Blue Backpack (Updated) ${runId}`,
            description: 'Navy blue JanSport with laptop and calculator',
            category: 'BAGS',
            location: 'Science Library 3rd Floor',
            status: 'LOST'
        }
    });
    if (editItemRes.status === 200 && editItemRes.data?.title?.includes('Updated')) {
        recordResult('Lost Item', 'Edit Item as Owner', 'PASS', { title: editItemRes.data.title });
    } else {
        recordResult('Lost Item', 'Edit Item as Owner', 'FAIL', { status: editItemRes.status, data: editItemRes.data });
    }

    // Cross-user Edit Protection
    const crossEdit = await request(`/api/items/${lostItemA.id}`, {
        method: 'PUT',
        token: userB_token,
        body: { title: 'Hacked by Bob' }
    });
    if (crossEdit.status === 401 || crossEdit.status === 403) {
        recordResult('Lost Item', 'Non-Owner Forbidden from Editing Item', 'PASS', { status: crossEdit.status });
    } else {
        recordResult('Lost Item', 'Non-Owner Forbidden from Editing Item', 'FAIL', { status: crossEdit.status, data: crossEdit.data });
    }

    // ----------------------------------------------------
    // 5. FOUND ITEM FUNCTIONALITY
    // ----------------------------------------------------
    logSection('5. FOUND ITEM FUNCTIONALITY');

    let foundItemB = null;
    const createFoundRes = await request('/api/items', {
        method: 'POST',
        token: userB_token,
        body: {
            title: `Found Graphing Calculator ${runId}`,
            description: 'TI-84 Plus CE Silver Edition with initials AS on back',
            category: 'ELECTRONICS',
            location: 'Student Center Cafeteria',
            status: 'FOUND',
            date: '2026-10-04',
            verificationQuestion: 'What initials are engraved on the back casing?'
        }
    });

    if (createFoundRes.status === 201 && createFoundRes.data?.id) {
        foundItemB = createFoundRes.data;
        recordResult('Found Item', 'Create FOUND Item Report (User B)', 'PASS', { id: foundItemB.id, title: foundItemB.title, status: foundItemB.status });
    } else {
        recordResult('Found Item', 'Create FOUND Item Report (User B)', 'FAIL', { status: createFoundRes.status, data: createFoundRes.data });
    }

    // Verify found item appears publicly
    const publicItems = await request('/api/items?size=100');
    const appearsInPublic = publicItems.data?.content?.some?.(i => i.id === foundItemB?.id) || publicItems.data?.some?.(i => i.id === foundItemB?.id);
    if (appearsInPublic) {
        recordResult('Found Item', 'Found Item Appears in Public Feed', 'PASS', { id: foundItemB.id });
    } else {
        recordResult('Found Item', 'Found Item Appears in Public Feed', 'FAIL', { publicData: publicItems.data });
    }

    // ----------------------------------------------------
    // 6. SEARCH / FILTER FUNCTIONALITY
    // ----------------------------------------------------
    logSection('6. SEARCH / FILTER FUNCTIONALITY');

    const searchKeyword = await request(`/api/items?keyword=Calculator&size=100`);
    const keywordMatches = searchKeyword.data?.content || searchKeyword.data || [];
    const keywordFound = keywordMatches.some(i => i.id === foundItemB.id);
    recordResult('Search', 'Filter by Keyword ("Calculator")', keywordFound ? 'PASS' : 'FAIL', { found: keywordFound, count: keywordMatches.length });

    const searchCategory = await request(`/api/items?category=ELECTRONICS&size=100`);
    const catMatches = searchCategory.data?.content || searchCategory.data || [];
    const catFound = catMatches.some(i => i.id === foundItemB.id);
    recordResult('Search', 'Filter by Category ("ELECTRONICS")', catFound ? 'PASS' : 'FAIL', { found: catFound, count: catMatches.length });

    const searchStatus = await request(`/api/items?status=FOUND&size=100`);
    const statusMatches = searchStatus.data?.content || searchStatus.data || [];
    const statusFound = statusMatches.some(i => i.id === foundItemB.id);
    recordResult('Search', 'Filter by Status ("FOUND")', statusFound ? 'PASS' : 'FAIL', { found: statusFound, count: statusMatches.length });

    const searchCombined = await request(`/api/items?keyword=Calculator&category=ELECTRONICS&status=FOUND&size=100`);
    const combMatches = searchCombined.data?.content || searchCombined.data || [];
    const combFound = combMatches.some(i => i.id === foundItemB.id);
    recordResult('Search', 'Combined Filter (Keyword + Category + Status)', combFound ? 'PASS' : 'FAIL', { found: combFound, count: combMatches.length });

    const searchNonExistent = await request(`/api/items?keyword=UnobtainiumXYZ9999`);
    const noneMatches = searchNonExistent.data?.content || searchNonExistent.data || [];
    recordResult('Search', 'Search Nonexistent Item (0 results)', noneMatches.length === 0 ? 'PASS' : 'FAIL', { count: noneMatches.length });

    // ----------------------------------------------------
    // 7. ITEM DETAILS
    // ----------------------------------------------------
    logSection('7. ITEM DETAILS');

    const detailsRes = await request(`/api/items/${foundItemB.id}`);
    if (detailsRes.status === 200 && detailsRes.data?.id === foundItemB.id) {
        const itemObj = detailsRes.data;
        const hasSensitives = 'password' in itemObj || (itemObj.user && 'password' in itemObj.user);
        recordResult('Item Details', 'Item Detail Retrieval & Sensitivity Check', !hasSensitives ? 'PASS' : 'FAIL', {
            title: itemObj.title,
            category: itemObj.category,
            status: itemObj.status,
            verificationQuestion: itemObj.verificationQuestion,
            sensitiveFieldsExposed: hasSensitives
        });
    } else {
        recordResult('Item Details', 'Item Detail Retrieval', 'FAIL', { status: detailsRes.status });
    }

    // ----------------------------------------------------
    // 8. CLAIM SYSTEM
    // ----------------------------------------------------
    logSection('8. CLAIM SYSTEM');

    // Finder attempts to claim own item (User B claims foundItemB)
    const selfClaim = await request('/api/claims', {
        method: 'POST',
        token: userB_token,
        body: {
            itemId: foundItemB.id,
            proofDescription: 'I found this so it is mine',
            verificationAnswer: 'None'
        }
    });
    if (selfClaim.status === 400) {
        recordResult('Claims', 'Finder Prevented from Claiming Own Item', 'PASS', { status: 400 });
    } else {
        recordResult('Claims', 'Finder Prevented from Claiming Own Item', 'FAIL', { status: selfClaim.status });
    }

    // User A claims foundItemB
    let claim1 = null;
    const claimResA = await request('/api/claims', {
        method: 'POST',
        token: userA_token,
        body: {
            itemId: foundItemB.id,
            proofDescription: 'I left my TI-84 in the cafeteria at 1pm. It has yellow rubber feet.',
            verificationAnswer: 'AS'
        }
    });

    if (claimResA.status === 201 && claimResA.data?.id) {
        claim1 = claimResA.data;
        recordResult('Claims', 'User A Files Claim against User B Item', 'PASS', { claimId: claim1.id, status: claim1.status });
    } else {
        recordResult('Claims', 'User A Files Claim against User B Item', 'FAIL', { status: claimResA.status, data: claimResA.data });
    }

    // Duplicate Claim Attempt by User A
    const dupClaim = await request('/api/claims', {
        method: 'POST',
        token: userA_token,
        body: {
            itemId: foundItemB.id,
            proofDescription: 'Filing again just in case',
            verificationAnswer: 'AS'
        }
    });
    if (dupClaim.status === 400) {
        recordResult('Claims', 'Duplicate Pending Claim Prevented', 'PASS', { status: 400 });
    } else {
        recordResult('Claims', 'Duplicate Pending Claim Prevented', 'FAIL', { status: dupClaim.status });
    }

    // Claim Comments
    const commentRes = await request(`/api/claims/${claim1.id}/comments`, {
        method: 'POST',
        token: userB_token,
        body: { message: 'Can you confirm the color of the sliding cover?' }
    });
    if (commentRes.status === 201) {
        recordResult('Claims', 'Add Comment to Claim Thread', 'PASS', { commentId: commentRes.data?.id });
    } else {
        recordResult('Claims', 'Add Comment to Claim Thread', 'FAIL', { status: commentRes.status });
    }

    const getComments = await request(`/api/claims/${claim1.id}/comments`, { token: userA_token });
    if (getComments.status === 200 && getComments.data?.length > 0) {
        recordResult('Claims', 'Read Comments Thread', 'PASS', { count: getComments.data.length });
    } else {
        recordResult('Claims', 'Read Comments Thread', 'FAIL', { status: getComments.status });
    }

    // ----------------------------------------------------
    // 9. CLAIM APPROVAL + HANDOVER PIN (CRITICAL)
    // ----------------------------------------------------
    logSection('9. CLAIM APPROVAL + HANDOVER PIN (PATH A: STUDENT APPROVAL)');

    // Path A: User B (finder) approves User A's claim
    const approveA = await request(`/api/claims/${claim1.id}/status?status=APPROVED`, {
        method: 'PATCH',
        token: userB_token
    });

    let generatedPin = null;
    if (approveA.status === 200) {
        recordResult('Approval Path A', 'Student Approves Claim Status to APPROVED', 'PASS', { status: approveA.data?.status });
        // Check item status
        const itemAfterApprove = await request(`/api/items/${foundItemB.id}`);
        recordResult('Approval Path A', 'Item Transitions to CLAIMED Status', itemAfterApprove.data?.status === 'CLAIMED' ? 'PASS' : 'FAIL', { status: itemAfterApprove.data?.status });

        // Check claimant sees PIN
        const claimantClaimView = await request(`/api/claims/${claim1.id}`, { token: userA_token });
        generatedPin = claimantClaimView.data?.handoverPin || claimantClaimView.data?.handoverCode;
        const pinValid = /^\d{6}$/.test(generatedPin);
        recordResult('Approval Path A', 'Handover PIN Generated and Visible to Claimant (6 digits)', pinValid ? 'PASS' : 'FAIL', { pin: generatedPin });

        // Check unauthorized user C cannot see PIN
        const intruderClaimView = await request(`/api/claims/${claim1.id}`, { token: userC_token });
        const intruderPin = intruderClaimView.data?.handoverPin || intruderClaimView.data?.handoverCode;
        recordResult('Approval Path A', 'Unauthorized User C Cannot See Handover PIN', intruderPin === null || intruderPin === undefined ? 'PASS' : 'FAIL', { intruderPin });

        // Persistence test: Re-query claim, PIN must be identical
        const claimantClaimReload = await request(`/api/claims/${claim1.id}`, { token: userA_token });
        const reloadedPin = claimantClaimReload.data?.handoverPin || claimantClaimReload.data?.handoverCode;
        recordResult('Approval Path A', 'PIN Persists Unchanged on Subsequent Fetches', reloadedPin === generatedPin ? 'PASS' : 'FAIL', { original: generatedPin, reloaded: reloadedPin });

        // Handover PIN Verification
        // 1. Wrong PIN
        const wrongPinRes = await request(`/api/claims/item/${foundItemB.id}/verify-pin`, {
            method: 'POST',
            token: userB_token,
            body: { pin: '000000' }
        });
        recordResult('Approval Path A', 'Incorrect Handover PIN is Rejected', wrongPinRes.status === 400 ? 'PASS' : 'FAIL', { status: wrongPinRes.status, msg: wrongPinRes.data?.message });

        // 2. Intruder attempts PIN verification
        const intruderVerify = await request(`/api/claims/item/${foundItemB.id}/verify-pin`, {
            method: 'POST',
            token: userC_token,
            body: { pin: generatedPin }
        });
        recordResult('Approval Path A', 'Unauthorized User Cannot Verify Handover PIN', intruderVerify.status === 401 || intruderVerify.status === 403 ? 'PASS' : 'FAIL', { status: intruderVerify.status });

        // 3. Correct PIN by finder
        const correctPinRes = await request(`/api/claims/item/${foundItemB.id}/verify-pin`, {
            method: 'POST',
            token: userB_token,
            body: { pin: generatedPin }
        });
        if (correctPinRes.status === 200 && correctPinRes.data?.handoverVerified === true) {
            recordResult('Approval Path A', 'Correct Handover PIN Accepted & Handover Verified', 'PASS', { verified: correctPinRes.data.handoverVerified });
        } else {
            recordResult('Approval Path A', 'Correct Handover PIN Accepted & Handover Verified', 'FAIL', { status: correctPinRes.status, data: correctPinRes.data });
        }

        // Verify final item status is REUNITED
        const itemFinal = await request(`/api/items/${foundItemB.id}`);
        recordResult('Approval Path A', 'Item Status Updated to REUNITED Post-Handover', itemFinal.data?.status === 'REUNITED' ? 'PASS' : 'FAIL', { status: itemFinal.data?.status });

        // Cannot claim REUNITED item
        const reClaim = await request('/api/claims', {
            method: 'POST',
            token: userC_token,
            body: { itemId: foundItemB.id, proofDescription: 'Try claiming reunited item' }
        });
        recordResult('Approval Path A', 'REUNITED Item Cannot Be Claimed Again', reClaim.status === 400 ? 'PASS' : 'FAIL', { status: reClaim.status });

    } else {
        recordResult('Approval Path A', 'Student Approves Claim Status to APPROVED', 'FAIL', { status: approveA.status });
    }

    // ----------------------------------------------------
    // PATH B: ADMIN CLAIM APPROVAL + HANDOVER VERIFICATION (BUG #1 FIX VERIFICATION)
    // ----------------------------------------------------
    logSection('9. CLAIM APPROVAL + HANDOVER PIN (PATH B: ADMIN APPROVAL - BUG #1 FIX)');

    // 1. Create Found Item 2 by User A
    const found2Res = await request('/api/items', {
        method: 'POST',
        token: userA_token,
        body: {
            title: `Found Keychain with 3 Keys ${runId}`,
            description: 'Brass carabiner with Honda car key',
            category: 'KEYS',
            location: 'Gymnasium Locker Room',
            status: 'FOUND',
            date: '2026-10-04'
        }
    });
    const foundItem2 = found2Res.data;

    // 2. User B submits claim on Item 2
    const claim2Res = await request('/api/claims', {
        method: 'POST',
        token: userB_token,
        body: {
            itemId: foundItem2.id,
            proofDescription: 'Lost my car keys after workout',
            verificationAnswer: 'Honda key with blue gym fob'
        }
    });
    const claim2 = claim2Res.data;

    // 3. Admin approves claim via /api/admin/claims/{id}/approve
    const adminApprove = await request(`/api/admin/claims/${claim2.id}/approve`, {
        method: 'PATCH',
        token: admin_token
    });

    if (adminApprove.status === 200) {
        recordResult('Approval Path B (Admin)', 'Admin Approves Claim (/api/admin/claims/{id}/approve)', 'PASS', { status: 200 });

        // 4. Verify item status transitioned to CLAIMED
        const item2AfterApprove = await request(`/api/items/${foundItem2.id}`);
        recordResult('Approval Path B (Admin)', 'Item Transitions to CLAIMED Status upon Admin Approval', item2AfterApprove.data?.status === 'CLAIMED' ? 'PASS' : 'FAIL', { status: item2AfterApprove.data?.status });

        // 5. Inspect claim from Claimant perspective (User B)
        const claimantView2 = await request(`/api/claims/${claim2.id}`, { token: userB_token });
        const adminGenPin = claimantView2.data?.handoverPin || claimantView2.data?.handoverCode;
        const pinValid = /^\d{6}$/.test(adminGenPin);

        recordResult('Approval Path B (Admin)', 'BUG #1 FIXED: Admin Approval Generates 6-Digit Handover PIN', pinValid ? 'PASS' : 'FAIL', {
            claimId: claim2.id,
            status: claimantView2.data?.status,
            handoverPin: adminGenPin
        });

        // 6. Test PIN idempotency: repeated admin approval must NOT change the PIN
        await request(`/api/admin/claims/${claim2.id}/approve`, { method: 'PATCH', token: admin_token });
        const claimantViewAfterReapprove = await request(`/api/claims/${claim2.id}`, { token: userB_token });
        const reapprovedPin = claimantViewAfterReapprove.data?.handoverPin || claimantViewAfterReapprove.data?.handoverCode;
        recordResult('Approval Path B (Admin)', 'PIN Idempotency: Repeated Admin Approval Preserves Exact Same PIN', reapprovedPin === adminGenPin ? 'PASS' : 'FAIL', {
            originalPin: adminGenPin,
            afterReapprovalPin: reapprovedPin
        });

        // 7. Verify unauthorized student C cannot see the PIN
        const intruderViewClaim2 = await request(`/api/claims/${claim2.id}`, { token: userC_token });
        recordResult('Approval Path B (Admin)', 'Unauthorized Student Forbidden from Reading Admin-Approved Claim (403)', intruderViewClaim2.status === 403 ? 'PASS' : 'FAIL', { status: intruderViewClaim2.status });

        // 8. Test Handover Verification: Wrong PIN rejected (400)
        const wrongPinAdminPath = await request(`/api/claims/item/${foundItem2.id}/verify-pin`, {
            method: 'POST',
            token: userA_token,
            body: { pin: '000000' }
        });
        recordResult('Approval Path B (Admin)', 'Handover Verification Rejects Incorrect PIN', wrongPinAdminPath.status === 400 ? 'PASS' : 'FAIL', { status: wrongPinAdminPath.status });

        // 9. Test Handover Verification: Correct PIN accepted (200, handoverVerified = true)
        const correctPinAdminPath = await request(`/api/claims/item/${foundItem2.id}/verify-pin`, {
            method: 'POST',
            token: userA_token,
            body: { pin: adminGenPin }
        });
        recordResult('Approval Path B (Admin)', 'Handover Verification Accepts Admin-Generated PIN', correctPinAdminPath.status === 200 && correctPinAdminPath.data?.handoverVerified === true ? 'PASS' : 'FAIL', {
            status: correctPinAdminPath.status,
            verified: correctPinAdminPath.data?.handoverVerified
        });

        // 10. Verify final item status is REUNITED
        const item2Final = await request(`/api/items/${foundItem2.id}`);
        recordResult('Approval Path B (Admin)', 'Item Transitions to REUNITED Following Admin Handover Verification', item2Final.data?.status === 'REUNITED' ? 'PASS' : 'FAIL', { status: item2Final.data?.status });
    } else {
        recordResult('Approval Path B (Admin)', 'Admin Approves Claim', 'FAIL', { status: adminApprove.status });
    }

    // ----------------------------------------------------
    // 10. MESSAGING / COMMENTS
    // ----------------------------------------------------
    logSection('10. MESSAGING / COMMENTS');

    // Send Message User A to User B
    const sendMsg = await request('/api/messages', {
        method: 'POST',
        token: userA_token,
        body: {
            itemId: foundItemB.id,
            recipientId: userB_id,
            claimantId: userA_id,
            content: 'Hello, can we meet at the campus security desk?'
        }
    });
    if (sendMsg.status === 201 && sendMsg.data?.id) {
        recordResult('Messaging', 'Send 1-on-1 Message Between Claimant and Finder', 'PASS', { id: sendMsg.data.id, content: sendMsg.data.content });
    } else {
        recordResult('Messaging', 'Send 1-on-1 Message Between Claimant and Finder', 'FAIL', { status: sendMsg.status, data: sendMsg.data });
    }

    // Empty message rejection
    const emptyMsg = await request('/api/messages', {
        method: 'POST',
        token: userA_token,
        body: {
            itemId: foundItemB.id,
            recipientId: userB_id,
            content: '   '
        }
    });
    recordResult('Messaging', 'Empty Message Content Rejection', emptyMsg.status === 400 || emptyMsg.status === 500 ? 'PASS' : 'FAIL', { status: emptyMsg.status });

    // Read thread
    const threadRes = await request(`/api/messages/item/${foundItemB.id}/claimant/${userA_id}`, { token: userB_token });
    recordResult('Messaging', 'Retrieve Scoped Conversation Thread', threadRes.status === 200 && threadRes.data?.length > 0 ? 'PASS' : 'FAIL', { count: threadRes.data?.length });

    // Inbox check
    const inboxB = await request(`/api/messages/inbox/${userB_id}`, { token: userB_token });
    recordResult('Messaging', 'Recipient Inbox Retrieval', inboxB.status === 200 ? 'PASS' : 'FAIL', { unreadCount: inboxB.data?.length });

    // Unauthorized inbox access (User C tries to read User B inbox)
    const hackInbox = await request(`/api/messages/inbox/${userB_id}`, { token: userC_token });
    recordResult('Messaging', 'Intruder Forbidden from Reading Another User Inbox', hackInbox.status === 401 || hackInbox.status === 403 ? 'PASS' : 'FAIL', { status: hackInbox.status });

    // ----------------------------------------------------
    // 11. NOTIFICATIONS
    // ----------------------------------------------------
    logSection('11. NOTIFICATIONS');

    const notifsA = await request('/api/notifications', { token: userA_token });
    if (notifsA.status === 200 && notifsA.data?.length > 0) {
        recordResult('Notifications', 'User A Received Notifications', 'PASS', { count: notifsA.data.length, latest: notifsA.data[0].message });
        const notifId = notifsA.data[0].id;
        const markRead = await request(`/api/notifications/${notifId}/read`, { method: 'PATCH', token: userA_token });
        recordResult('Notifications', 'Mark Single Notification as Read', markRead.status === 200 ? 'PASS' : 'FAIL', { status: markRead.status });
    } else {
        recordResult('Notifications', 'User A Received Notifications', 'PARTIAL', { count: notifsA.data?.length || 0 });
    }

    const markAllRead = await request('/api/notifications/read-all', { method: 'PATCH', token: userA_token });
    recordResult('Notifications', 'Mark All Notifications as Read', markAllRead.status === 200 ? 'PASS' : 'FAIL', { status: markAllRead.status });

    // ----------------------------------------------------
    // 12. ADMIN DASHBOARD
    // ----------------------------------------------------
    logSection('12. ADMIN DASHBOARD');

    const adminStats = await request('/api/admin/stats', { token: admin_token });
    recordResult('Admin', 'Get Dashboard Statistics', adminStats.status === 200 ? 'PASS' : 'FAIL', adminStats.data);

    const adminItems = await request('/api/admin/items', { token: admin_token });
    recordResult('Admin', 'List All Items for Admin Audit', adminItems.status === 200 ? 'PASS' : 'FAIL', { count: adminItems.data?.length });

    const adminClaims = await request('/api/admin/claims', { token: admin_token });
    recordResult('Admin', 'List All Claims for Admin Audit', adminClaims.status === 200 ? 'PASS' : 'FAIL', { count: adminClaims.data?.length });

    const adminUsers = await request('/api/admin/users', { token: admin_token });
    recordResult('Admin', 'List All Users for Admin Management', adminUsers.status === 200 ? 'PASS' : 'FAIL', { count: adminUsers.data?.length });

    const adminExport = await request('/api/admin/claims/export', { token: admin_token });
    const isCsv = typeof adminExport.data === 'string' && (adminExport.data.includes('Claim ID,Item ID') || adminExport.data.includes('Claim ID'));
    recordResult('Admin', 'Export Claims Audit Report as CSV', isCsv ? 'PASS' : 'FAIL', { csvLength: adminExport.data?.length });

    // ----------------------------------------------------
    // 13. IMAGE UPLOAD
    // ----------------------------------------------------
    logSection('13. IMAGE UPLOAD');

    // Create a dummy PNG in memory
    const pngBuffer = Buffer.from('89504e470d0a1a0a0000000d49484452000000010000000108060000001f15c4890000000a49444154789c63000100000500010d0a2d450000000049454e44ae426082', 'hex');
    const formBoundary = '----WebKitFormBoundary' + Math.random().toString(36).substring(2);
    const multipartBody = Buffer.concat([
        Buffer.from(`--${formBoundary}\r\nContent-Disposition: form-data; name="file"; filename="test_upload.png"\r\nContent-Type: image/png\r\n\r\n`),
        pngBuffer,
        Buffer.from(`\r\n--${formBoundary}--\r\n`)
    ]);

    const uploadRes = await fetch(`${BASE_URL}/api/items/upload-image`, {
        method: 'POST',
        headers: {
            'Content-Type': `multipart/form-data; boundary=${formBoundary}`,
            'Authorization': `Bearer ${userA_token}`
        },
        body: multipartBody
    });

    let uploadedPath = null;
    if (uploadRes.ok) {
        const json = await uploadRes.json();
        uploadedPath = json.imageUrl;
        recordResult('Image Upload', 'Upload Valid PNG Image File', 'PASS', { imageUrl: uploadedPath });

        // Verify static asset serving
        const fetchAsset = await fetch(`${BASE_URL}${uploadedPath}`);
        const assetOk = fetchAsset.ok && (fetchAsset.headers.get('content-type') || '').includes('image');
        recordResult('Image Upload', 'Static Asset Handler Serves Uploaded Image with Correct MIME Type', assetOk ? 'PASS' : 'FAIL', {
            status: fetchAsset.status,
            contentType: fetchAsset.headers.get('content-type')
        });
    } else {
        recordResult('Image Upload', 'Upload Valid PNG Image File', 'FAIL', { status: uploadRes.status });
    }

    // ----------------------------------------------------
    // 14. SECURITY & AUTHORIZATION DEEP CHECK (BUGS #2, #3, #4)
    // ----------------------------------------------------
    logSection('14. SECURITY & AUTHORIZATION DEEP CHECK');

    // BUG #3 FIX: IDOR on Claim Details: User C reads User A's claim -> MUST RETURN 403 FORBIDDEN
    const idorClaim = await request(`/api/claims/${claim1.id}`, { token: userC_token });
    if (idorClaim.status === 403) {
        recordResult('Security', 'BUG #3 FIXED: IDOR Blocked - Non-Party Forbidden from Reading Claim (403)', 'PASS', {
            status: 403,
            message: idorClaim.data?.message || 'Access Denied'
        });
    } else {
        recordResult('Security', 'BUG #3 FIXED: IDOR Blocked - Non-Party Forbidden from Reading Claim (403)', 'FAIL', {
            status: idorClaim.status,
            data: idorClaim.data
        });
    }

    // Claimant (User A) and Finder (User B) CAN access the claim
    const claimantAccess = await request(`/api/claims/${claim1.id}`, { token: userA_token });
    const finderAccess = await request(`/api/claims/${claim1.id}`, { token: userB_token });
    recordResult('Security', 'Claimant & Finder Maintain Authorized Access to Claim', claimantAccess.status === 200 && finderAccess.status === 200 ? 'PASS' : 'FAIL', {
        claimantStatus: claimantAccess.status,
        finderStatus: finderAccess.status
    });

    // BUG #4 FIX: IDOR on Item Message Thread: User C reads Item 1 thread -> MUST RETURN 403 FORBIDDEN
    const idorMsg = await request(`/api/messages/item/${foundItemB.id}`, { token: userC_token });
    if (idorMsg.status === 403) {
        recordResult('Security', 'BUG #4 FIXED: Thread Authorization - Non-Participant Forbidden from Reading Messages (403)', 'PASS', {
            status: 403,
            message: idorMsg.data?.message || 'Access Denied'
        });
    } else {
        recordResult('Security', 'BUG #4 FIXED: Thread Authorization - Non-Participant Forbidden from Reading Messages (403)', 'FAIL', {
            status: idorMsg.status,
            data: idorMsg.data
        });
    }

    // BUG #2 FIX: UnauthorizedException returns HTTP 403 Forbidden (not HTTP 500)
    const crossDelete = await request(`/api/items/${foundItemB.id}`, {
        method: 'DELETE',
        token: userC_token
    });
    recordResult('Security', 'BUG #2 FIXED: Unauthorized Action Returns Clean HTTP 403 (Not 500)', crossDelete.status === 403 ? 'PASS' : 'FAIL', { status: crossDelete.status });

    // Student Deletes User Account via Admin Endpoint -> Returns 403 Forbidden
    const hackDeleteUser = await request(`/api/admin/users/${userA_id}`, {
        method: 'DELETE',
        token: userC_token
    });
    recordResult('Security', 'Student Forbidden from Deleting Users via /api/admin/users/{id}', hackDeleteUser.status === 403 ? 'PASS' : 'FAIL', { status: hackDeleteUser.status });

    // ----------------------------------------------------
    // SUMMARY
    // ----------------------------------------------------
    logSection('TEST EXECUTION SUMMARY');
    const total = results.length;
    const passed = results.filter(r => r.status === 'PASS').length;
    const failed = results.filter(r => r.status === 'FAIL').length;
    const partial = results.filter(r => r.status === 'PARTIAL').length;

    console.log(`Total Scenarios Tested: ${total}`);
    console.log(`Passed:  ${passed} (${Math.round((passed / total) * 100)}%)`);
    console.log(`Failed:  ${failed}`);
    console.log(`Partial: ${partial}`);

    fs.writeFileSync('e2e-test-results.json', JSON.stringify(results, null, 2));
    console.log(`Results saved to e2e-test-results.json`);
}

runAllTests().catch(err => {
    console.error('Fatal Test Runner Error:', err);
});
