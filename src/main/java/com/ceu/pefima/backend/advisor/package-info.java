/**
 * Investment profile review: fixed risk scenarios compared with the user's current
 * allocation, plus an optional comment from a local LLM (LM Studio). Enabled only
 * when {@code pefima.advisor.enabled=true}; the LLM must listen on a loopback
 * address, so profile figures never leave the machine. Nothing is stored.
 */
@NullMarked
package com.ceu.pefima.backend.advisor;

import org.jspecify.annotations.NullMarked;
