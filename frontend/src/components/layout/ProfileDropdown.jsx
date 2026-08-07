import React, { useState, useEffect, useContext, useRef } from 'react';
import { getUserProfile, updateProfile, submitRoleChangeRequest, changePassword } from '../../api/userService';
import { AuthContext } from '../../context/AuthContext';
import { motion, AnimatePresence } from 'framer-motion';
import './ProfileDropdown.css';

const ProfileDropdown = ({ open, onClose }) => {
    const { user, login } = useContext(AuthContext);
    const dropdownRef = useRef(null);

    const [profile, setProfile] = useState(null);
    const [loading, setLoading] = useState(true);
    const [activeSection, setActiveSection] = useState('view'); // 'view', 'edit', 'password', 'role'

    // Edit form
    const [form, setForm] = useState({ firstName: '', lastName: '', email: '', phone: '' });
    const [saving, setSaving] = useState(false);
    const [changingPassword, setChangingPassword] = useState(false);
    const [message, setMessage] = useState('');
    const [messageType, setMessageType] = useState(''); // 'success' or 'error'
    const [passwordForm, setPasswordForm] = useState({
        oldPassword: '',
        newPassword: '',
        confirmNewPassword: '',
    });

    // Role change form
    const [roleForm, setRoleForm] = useState({ requestedRole: 'RESTAURANT_OWNER', requestReason: '' });
    const [submittingRole, setSubmittingRole] = useState(false);

    const formatMoney = (value) => {
        if (value === null || value === undefined || value === '') return '—';
        const numeric = Number(value);
        if (Number.isNaN(numeric)) return '—';
        return `Rs ${numeric.toFixed(2)}`;
    };

    const walletBalance =
        profile?.walletBalance
        ?? profile?.wallet_balance
        ?? profile?.wallet
        ?? profile?.balance
        ?? user?.walletBalance
        ?? user?.wallet_balance
        ?? user?.wallet
        ?? user?.balance;

    useEffect(() => {
        if (open) {
            fetchProfile();
            setActiveSection('view');
            setMessage('');
        }
    }, [open]);

    // Close on outside click
    useEffect(() => {
        const handleClickOutside = (e) => {
            if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
                onClose();
            }
        };
        if (open) document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, [open, onClose]);

    const fetchProfile = async () => {
        setLoading(true);
        try {
            const data = await getUserProfile();
            setProfile(data);
            setForm({
                firstName: data.firstName || '',
                lastName: data.lastName || '',
                email: data.email || '',
                phone: data.phone || '',
            });
        } catch {
            flash('Failed to load profile.', 'error');
        } finally {
            setLoading(false);
        }
    };

    const flash = (msg, type = 'success') => {
        setMessage(msg);
        setMessageType(type);
        setTimeout(() => setMessage(''), 3000);
    };

    const handleUpdateProfile = async (e) => {
        e.preventDefault();
        setSaving(true);
        setMessage('');
        try {
            const updated = await updateProfile(form);
            setProfile(updated);
            // Also update localStorage user data so navbar reflects the change
            const storedUser = JSON.parse(localStorage.getItem('user'));
            if (storedUser) {
                storedUser.firstName = updated.firstName;
                storedUser.email = updated.email;
                login(storedUser);
            }
            flash('Profile updated successfully!');
            setActiveSection('view');
        } catch (err) {
            const msg = err.response?.data?.message || err.response?.data || 'Failed to update profile.';
            flash(typeof msg === 'string' ? msg : 'Failed to update profile.', 'error');
        } finally {
            setSaving(false);
        }
    };

    const handleRoleRequest = async (e) => {
        e.preventDefault();
        if (!roleForm.requestReason.trim()) {
            flash('Please provide a reason for your request.', 'error');
            return;
        }
        setSubmittingRole(true);
        setMessage('');
        try {
            await submitRoleChangeRequest(roleForm);
            flash('Role change request submitted! We\'ll get back to you soon.');
            setRoleForm({ requestedRole: 'RESTAURANT_OWNER', requestReason: '' });
            setActiveSection('view');
        } catch (err) {
            const msg = err.response?.data?.message || err.response?.data || 'Failed to submit request.';
            flash(typeof msg === 'string' ? msg : 'Failed to submit request.', 'error');
        } finally {
            setSubmittingRole(false);
        }
    };

    const handleChangePassword = async (e) => {
        e.preventDefault();
        if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.confirmNewPassword) {
            flash('Please fill all password fields.', 'error');
            return;
        }
        if (passwordForm.newPassword.length < 8) {
            flash('New password must be at least 8 characters.', 'error');
            return;
        }
        if (passwordForm.newPassword !== passwordForm.confirmNewPassword) {
            flash('New password and confirm password must match.', 'error');
            return;
        }

        setChangingPassword(true);
        try {
            const response = await changePassword(passwordForm);
            flash(typeof response === 'string' ? response : 'Password updated successfully!');
            setPasswordForm({ oldPassword: '', newPassword: '', confirmNewPassword: '' });

            const storedUser = JSON.parse(localStorage.getItem('user'));
            if (storedUser?.isTempPassword) {
                login({ ...storedUser, isTempPassword: false });
            }

            setActiveSection('view');
        } catch (err) {
            const msg = err.response?.data?.message || err.response?.data || 'Failed to change password.';
            flash(typeof msg === 'string' ? msg : 'Failed to change password.', 'error');
        } finally {
            setChangingPassword(false);
        }
    };

    if (!open) return null;

    return (
        <AnimatePresence>
            <motion.div
                ref={dropdownRef}
                className="profile-dropdown"
                initial={{ opacity: 0, y: -10, scale: 0.95 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                exit={{ opacity: 0, y: -10, scale: 0.95 }}
                transition={{ duration: 0.2 }}
            >
                {/* Header */}
                <div className="pd-header">
                    <div className="pd-avatar">
                        {profile?.firstName?.charAt(0)?.toUpperCase() || '?'}
                    </div>
                    <div className="pd-header-info">
                        <span className="pd-name">{profile?.firstName || ''} {profile?.lastName || ''}</span>
                        <span className="pd-email">{profile?.email || ''}</span>
                        <span className="pd-role">{profile?.role?.replace('_', ' ') || ''}</span>
                    </div>
                    <button className="pd-close" onClick={onClose}>✕</button>
                </div>

                {/* Toast */}
                {message && (
                    <div className={`pd-toast ${messageType === 'error' ? 'pd-toast-error' : 'pd-toast-success'}`}>
                        {message}
                    </div>
                )}

                {loading ? (
                    <div className="pd-loading">Loading profile...</div>
                ) : (
                    <>
                        {/* ── VIEW MODE ── */}
                        {activeSection === 'view' && (
                            <div className="pd-view">
                                <div className="pd-field-row">
                                    <div className="pd-field">
                                        <span className="pd-field-label">First Name</span>
                                        <span className="pd-field-value">{profile?.firstName || '—'}</span>
                                    </div>
                                    <div className="pd-field">
                                        <span className="pd-field-label">Last Name</span>
                                        <span className="pd-field-value">{profile?.lastName || '—'}</span>
                                    </div>
                                </div>
                                <div className="pd-field">
                                    <span className="pd-field-label">Email</span>
                                    <span className="pd-field-value">{profile?.email || '—'}</span>
                                </div>
                                <div className="pd-field">
                                    <span className="pd-field-label">Phone</span>
                                    <span className="pd-field-value">{profile?.phone || '—'}</span>
                                </div>
                                <div className="pd-field">
                                    <span className="pd-field-label">Wallet Balance</span>
                                    <span className="pd-field-value">{formatMoney(walletBalance)}</span>
                                </div>

                                <div className="pd-actions">
                                    <button className="pd-btn pd-btn-primary" onClick={() => setActiveSection('edit')}>
                                        ✏️ Edit Profile
                                    </button>
                                    <button className="pd-btn pd-btn-outline" onClick={() => setActiveSection('password')}>
                                        🔒 Change Password
                                    </button>
                                </div>

                                {user?.isTempPassword && (
                                    <div className="pd-temp-password-note">
                                        Temporary password detected. Please change your password now.
                                    </div>
                                )}

                                {/* Role change CTA — only for CUSTOMER */}
                                {user?.role === 'CUSTOMER' && (
                                    <div className="pd-role-cta" onClick={() => setActiveSection('role')}>
                                        <span className="pd-role-cta-icon">🤝</span>
                                        <div className="pd-role-cta-text">
                                            <span className="pd-role-cta-title">Want to join us, to remove hunger?</span>
                                            <span className="pd-role-cta-sub">Apply for a role change →</span>
                                        </div>
                                    </div>
                                )}
                            </div>
                        )}

                        {/* ── EDIT MODE ── */}
                        {activeSection === 'edit' && (
                            <form className="pd-edit-form" onSubmit={handleUpdateProfile}>
                                <div className="pd-field-row">
                                    <div className="pd-input-group">
                                        <label>First Name</label>
                                        <input
                                            type="text"
                                            value={form.firstName}
                                            onChange={e => setForm({ ...form, firstName: e.target.value })}
                                            placeholder={profile?.firstName || 'First Name'}
                                            required
                                        />
                                    </div>
                                    <div className="pd-input-group">
                                        <label>Last Name</label>
                                        <input
                                            type="text"
                                            value={form.lastName}
                                            onChange={e => setForm({ ...form, lastName: e.target.value })}
                                            placeholder={profile?.lastName || 'Last Name'}
                                            required
                                        />
                                    </div>
                                </div>
                                <div className="pd-input-group">
                                    <label>Email</label>
                                    <input
                                        type="email"
                                        value={form.email}
                                        onChange={e => setForm({ ...form, email: e.target.value })}
                                        placeholder={profile?.email || 'Email'}
                                        required
                                    />
                                </div>
                                <div className="pd-input-group">
                                    <label>Phone</label>
                                    <input
                                        type="tel"
                                        value={form.phone}
                                        onChange={e => setForm({ ...form, phone: e.target.value })}
                                        placeholder={profile?.phone || 'Phone (10 digits)'}
                                        pattern="\d{10}"
                                        title="Phone must be 10 digits"
                                        required
                                    />
                                </div>

                                <div className="pd-actions">
                                    <button type="submit" className="pd-btn pd-btn-primary" disabled={saving}>
                                        {saving ? 'Saving...' : 'Save Changes'}
                                    </button>
                                    <button type="button" className="pd-btn pd-btn-outline" onClick={() => setActiveSection('password')}>
                                        Change Password
                                    </button>
                                    <button type="button" className="pd-btn pd-btn-outline" onClick={() => {
                                        setActiveSection('view');
                                        // Reset form to current profile
                                        if (profile) setForm({
                                            firstName: profile.firstName || '',
                                            lastName: profile.lastName || '',
                                            email: profile.email || '',
                                            phone: profile.phone || '',
                                        });
                                    }}>
                                        Cancel
                                    </button>
                                </div>
                            </form>
                        )}

                        {/* ── PASSWORD MODE ── */}
                        {activeSection === 'password' && (
                            <form className="pd-edit-form" onSubmit={handleChangePassword}>
                                <div className="pd-input-group">
                                    <label>Current Password</label>
                                    <input
                                        type="password"
                                        value={passwordForm.oldPassword}
                                        onChange={e => setPasswordForm({ ...passwordForm, oldPassword: e.target.value })}
                                        required
                                    />
                                </div>
                                <div className="pd-input-group">
                                    <label>New Password</label>
                                    <input
                                        type="password"
                                        value={passwordForm.newPassword}
                                        onChange={e => setPasswordForm({ ...passwordForm, newPassword: e.target.value })}
                                        minLength={8}
                                        required
                                    />
                                </div>
                                <div className="pd-input-group">
                                    <label>Confirm New Password</label>
                                    <input
                                        type="password"
                                        value={passwordForm.confirmNewPassword}
                                        onChange={e => setPasswordForm({ ...passwordForm, confirmNewPassword: e.target.value })}
                                        minLength={8}
                                        required
                                    />
                                </div>

                                <div className="pd-actions">
                                    <button type="submit" className="pd-btn pd-btn-primary" disabled={changingPassword}>
                                        {changingPassword ? 'Updating...' : 'Update Password'}
                                    </button>
                                    <button type="button" className="pd-btn pd-btn-outline" onClick={() => {
                                        setPasswordForm({ oldPassword: '', newPassword: '', confirmNewPassword: '' });
                                        setActiveSection('view');
                                    }}>
                                        Cancel
                                    </button>
                                </div>
                            </form>
                        )}

                        {/* ── ROLE REQUEST MODE ── */}
                        {activeSection === 'role' && (
                            <form className="pd-role-form" onSubmit={handleRoleRequest}>
                                <div className="pd-role-banner">
                                    <span className="pd-role-banner-emoji">🍽️</span>
                                    <span className="pd-role-banner-text">Want to join us, to remove hunger?</span>
                                </div>

                                <div className="pd-input-group">
                                    <label>Request Role</label>
                                    <select
                                        value={roleForm.requestedRole}
                                        onChange={e => setRoleForm({ ...roleForm, requestedRole: e.target.value })}
                                    >
                                        <option value="RESTAURANT_OWNER">Restaurant Owner</option>
                                    </select>
                                </div>

                                <div className="pd-input-group">
                                    <label>Why do you want to join?</label>
                                    <textarea
                                        value={roleForm.requestReason}
                                        onChange={e => setRoleForm({ ...roleForm, requestReason: e.target.value })}
                                        placeholder="Tell us about yourself and why you'd like to partner with Flocko..."
                                        rows={3}
                                        required
                                    />
                                </div>

                                <div className="pd-actions">
                                    <button type="submit" className="pd-btn pd-btn-primary" disabled={submittingRole}>
                                        {submittingRole ? 'Submitting...' : 'Submit Request'}
                                    </button>
                                    <button type="button" className="pd-btn pd-btn-outline" onClick={() => setActiveSection('view')}>
                                        Back
                                    </button>
                                </div>
                            </form>
                        )}
                    </>
                )}
            </motion.div>
        </AnimatePresence>
    );
};

export default ProfileDropdown;

