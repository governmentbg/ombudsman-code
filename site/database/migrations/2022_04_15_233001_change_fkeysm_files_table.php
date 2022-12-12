<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class ChangeFkeysmFilesTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::table('m_files', function (Blueprint $table) {
            $table->dropForeign('m_files_mn_id_foreign');
            $table->dropForeign('m_files_mv_id_foreign');

            $table->renameColumn('Mv_id', 'MvL_id');
            $table->renameColumn('Mn_id', 'MnL_id');






            $table->foreign('MnL_id')->references('MnL_id')->on('m_news_lng');
            $table->foreign('MvL_id')->references('MvL_id')->on('m_event_lng');
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::table('m_files', function (Blueprint $table) {
            //
        });
    }
}
