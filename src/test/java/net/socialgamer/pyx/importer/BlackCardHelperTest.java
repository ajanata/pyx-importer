/**
 * Copyright (c) 2018, Andy Janata
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this list of conditions
 *   and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice, this list of
 *   conditions and the following disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.socialgamer.pyx.importer;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;


public class BlackCardHelperTest {

  private BlackCardHelper helper;

  @Before
  public void before() {
    helper = new BlackCardHelper();
  }

  @Test
  public void testPick_NoBlank() {
    assertEquals(1, helper.pick("Simple card with no blanks."));
  }

  @Test
  public void testPick_UnderscoresButNotBlank() {
    assertEquals(1, helper.pick("This _ has __ underscores ___ but none of them are blanks."));
  }

  @Test
  public void testPick_BlankInMiddle() {
    assertEquals(1, helper.pick("This has a ____ in the middle."));
  }

  @Test
  public void testPick_BlankAtEndNoPunct() {
    assertEquals(1, helper.pick("This has a blank at the end without punctuation ____"));
  }

  @Test
  public void testPick_BlankAtEndPunct() {
    assertEquals(1, helper.pick("This has a blank at the end, ____."));
    assertEquals(1, helper.pick("This has a blank at the end, ____?"));
    assertEquals(1, helper.pick("This has a blank at the end, ____!"));
    assertEquals(1, helper.pick("This has a blank at the end, ____?!"));
  }

  @Test
  public void testPick_MultipleBlanks2() {
    assertEquals(2, helper.pick("This has ____ multiple ____, so here we go."));
    assertEquals(2, helper.pick("This has ____ multiple ____."));
    assertEquals(2, helper.pick("This has ____ multiple ____"));
    assertEquals(2, helper.pick("____ multiple ____, so here we go."));
    assertEquals(2, helper.pick("____ multiple ____"));
  }

  @Test
  public void testPick_MultipleBlanks3() {
    assertEquals(3, helper.pick("This ____ has ____ multiple ____, so here we go."));
    assertEquals(3, helper.pick("This ____ has ____ multiple ____."));
    assertEquals(3, helper.pick("This ____ has ____ multiple ____"));
    assertEquals(3, helper.pick("____ multiple ____, so ____ we go."));
    assertEquals(3, helper.pick("____ multiple ____? ____"));
  }

  @Test
  public void testDraw() {
    assertEquals(0, helper.draw("pick 1"));
    assertEquals(0, helper.draw("pick 2 ____ ____"));
    assertEquals(2, helper.draw("pick 3 ____ ____ ____"));
    assertEquals(3, helper.draw("pick 4 ____ ____ ____ ____"));
  }
}
