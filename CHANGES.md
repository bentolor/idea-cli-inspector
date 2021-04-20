<table>
<colgroup>
<col style="width: 14%" />
<col style="width: 85%" />
</colgroup>
<thead>
<tr class="header">
<th>Version</th>
<th>Change</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><p>1.7</p></td>
<td><ul>
<li><p>Support for Docker-based execution. Available on Docker Hub.</p></li>
<li><p>The <code>-rf</code> option no longer expects any IDEA configuration (<code>.idea</code>) to be present in the project.</p></li>
<li><p>More robust processing: No longer need to pass only absolute paths to <code>-rf</code> or <code>-p</code></p></li>
</ul></td>
</tr>
<tr class="even">
<td><p>1.6</p></td>
<td><ul>
<li><p>New <code>--rootfile</code> option to allow inspection of maven projects without <code>.iml</code> files committed to SCM.<br />
</p></li>
<li><p>The <code>--profile</code> option now accepts an absolute path to the inspection profile xml file. If the path is not absolute, then the fle is supposed to be in <code>rootdir/.idea/inspectionProfiles/</code>.</p></li>
<li><p>Dry-run option <code>-n</code> now does no longer touch the <code>idea.properties</code> file.</p></li>
<li><p>The <code>-v</code> option now also increases the verbosity of the IDEA process.</p></li>
<li><p>Add example for usage with Travis CI / Docker</p></li>
<li><p><strong>Eat your own dogfood</strong> - <code>idea-cli-inspector</code> now tests itself via Travis CI using Gradle &amp; <code>idea-cli-inspector</code></p></li>
<li><p>Fix problem on parsing values with contains <code>:</code>, i.e. like the path to the IDEA installation directory in windows like <code>C:\Program Files\…</code>. Thanks to @ColmBhandal. Fixes #19</p></li>
</ul></td>
</tr>
<tr class="odd">
<td><p>1.5.4</p></td>
<td><ul>
<li><p>CLI options override config values inside the <code>.ideainspect</code> file.</p></li>
<li><p>The <code>--verbose</code> option now prints the effective configuration.</p></li>
<li><p>Add documentation about scoping.<br />
</p></li>
<li><p>Add more FAQ entries.</p></li>
</ul></td>
</tr>
<tr class="even">
<td><p>1.5.3</p></td>
<td><ul>
<li><p>Render reported violations to STDERR. This should display them more prominently i.e. in CI environments.</p></li>
</ul></td>
</tr>
<tr class="odd">
<td><p>1.5.2</p></td>
<td><ul>
<li><p>Critical bugfix: Return code not returned on failed analysis. Introduced with 1.5.0.</p></li>
</ul></td>
</tr>
<tr class="even">
<td><p>1.5.1</p></td>
<td><ul>
<li><p>Increase process timeout to avoid seeing an unexpected return code of 143</p></li>
</ul></td>
</tr>
<tr class="odd">
<td><p>1.5</p></td>
<td><ul>
<li><p>Support for custom Intellij scopes<br />
</p></li>
</ul></td>
</tr>
<tr class="even">
<td><p>1.4</p></td>
<td><ul>
<li><p>Support for Android Studio installations<br />
</p></li>
</ul></td>
</tr>
<tr class="odd">
<td><p>1.3</p></td>
<td><ul>
<li><p>Support for Mac OSX locations of IntelliJ IDEA executables<br />
</p></li>
</ul></td>
</tr>
<tr class="even">
<td><p>1.2</p></td>
<td><ul>
<li><p>Add support for configuration file</p></li>
<li><p>Add debugging flag <code>-v</code></p></li>
<li><p>Replaced all mandatory CLI options with default values (root directory, IDEA home, Inspection profile name, …​)</p></li>
</ul></td>
</tr>
<tr class="odd">
<td><p>1.1</p></td>
<td><ul>
<li><p>Support for ignoring issues affecting specific source files using a regular expression (Option <code>-sf</code>)</p></li>
</ul></td>
</tr>
<tr class="even">
<td><p>1.0</p></td>
<td><ul>
<li><p>First release</p></li>
</ul></td>
</tr>
</tbody>
</table>
